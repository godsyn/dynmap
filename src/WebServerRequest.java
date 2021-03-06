import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class WebServerRequest extends Thread {
	protected static final Logger log = Logger.getLogger("Minecraft");

	private Socket sock;
	private MapManager mgr;

	public WebServerRequest(Socket socket, MapManager mgr)
	{
		sock = socket;
		this.mgr = mgr;
	}

	private static void sendHeader(BufferedOutputStream out, int code, String contentType, long contentLength, long lastModified) throws IOException
	{
		out.write(("HTTP/1.0 " + code + " OK\r\n" + 
					"Date: " + new Date().toString() + "\r\n" +
					"Server: JibbleWebServer/1.0\r\n" +
					"Content-Type: " + contentType + "\r\n" +
					"Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n" +
					((contentLength != -1) ? "Content-Length: " + contentLength + "\r\n" : "") +
					"Last-modified: " + new Date(lastModified).toString() + "\r\n" +
					"\r\n").getBytes());
	}

	private static void sendError(BufferedOutputStream out, int code, String message) throws IOException
	{
		message = message + "<hr>" + WebServer.VERSION;
		sendHeader(out, code, "text/html", message.length(), System.currentTimeMillis());
		out.write(message.getBytes());
		out.flush();
		out.close();
	}

	public void run()
	{
		InputStream reader = null;
		try {
			sock.setSoTimeout(30000);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());

			String request = in.readLine();
			if (request == null || !request.startsWith("GET ") || !(request.endsWith(" HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
				// Invalid request type (no "GET")
				sendError(out, 500, "Invalid Method.");
				return;
			}

			String path = request.substring(4, request.length() - 9);

			int current = (int) (System.currentTimeMillis() / 1000);
			long cutoff = 0;

			if(path.charAt(0) == '/') {
				try {
					cutoff = ((long) Integer.parseInt(path.substring(1))) * 1000;
				} catch(NumberFormatException e) {
				}
			}

			sendHeader(out, 200, "text/plain", -1, System.currentTimeMillis());

			StringBuilder sb = new StringBuilder();
			sb.append(current + " " + etc.getServer().getRelativeTime() + "\n");

			if (mgr.showPlayers) {
				for(Player player : etc.getServer().getPlayerList()) {
					sb.append(player.getName() + " player " + player.getX() + " " + player.getY() + " " + player.getZ() + "\n");
				}
			}
			
			if (mgr.showSigns) {
				for(Warp sign : mgr.signs.values())
				{
					sb.append(sign.Name + " sign " + sign.Location.x + " " + sign.Location.y + " " + sign.Location.z + "\n");
				}
			}

			if (mgr.showWarps) {
				List<Warp> warps = mgr.loadWarps();
				
				if (warps != null) {
					for(Warp warp : warps) {
						sb.append(warp.Name + " warp " + warp.Location.x + " " + warp.Location.y + " " + warp.Location.z + "\n");
					}
				}
			}
			
			if (mgr.showHomes) {
				List<Warp> homes = mgr.loadHomes();
				
				if (homes != null) {
					for(Warp warp : homes) {
						sb.append(warp.Name + " home " + warp.Location.x + " " + warp.Location.y + " " + warp.Location.z + "\n");
					}
				}
			}
			
			if (mgr.showSpawn) {
				Location spawnLocation = etc.getServer().getSpawnLocation();
				
				sb.append("Spawn spawn " + spawnLocation.x + " " + spawnLocation.y + " " + spawnLocation.z + "\n");
			}
			
			synchronized(mgr.lock) {
				for(TileUpdate tu : mgr.tileUpdates) {
					if(tu.at >= cutoff) {
						sb.append(tu.tile.px + "_" + tu.tile.py + " " + tu.tile.zpx + "_" + tu.tile.zpy + " t\n");
					}
				}

				for(TileUpdate tu : mgr.caveTileUpdates) {
					if(tu.at >= cutoff) {
						sb.append(tu.tile.px + "_" + tu.tile.py + " " + tu.tile.zpx + "_" + tu.tile.zpy + " c\n");
					}
				}
			}

			out.write(sb.toString().getBytes());

			out.flush();
			out.close();
		}
		catch (IOException e) {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception anye) {
					// Do nothing.
				}
			}
		}
	}
}
