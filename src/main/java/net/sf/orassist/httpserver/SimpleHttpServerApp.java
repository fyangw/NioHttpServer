package net.sf.orassist.httpserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleHttpServerApp implements Runnable {

	private String docRoot = "htdocs";
	private Socket client;

	public SimpleHttpServerApp(Socket client) {
		this.client = client;
	}

	public void run() {
		BufferedReader in = null;
		PrintStream out = null;
		String cmdLine = null;
        try {
        	try {
	            in = new BufferedReader(new InputStreamReader(client.getInputStream(), "utf-8"));
	            out = new PrintStream(client.getOutputStream());
	
	            cmdLine = in.readLine();
	            String[] cmd = cmdLine.split(" ");
	            
				File file = new File(docRoot + (cmd[1].endsWith("/") ? cmd[1] + "index.html" : cmd[1]));
				if (!cmd[0].equals("GET") || !file.exists()) {
					out.println("HTTP/1.0 " + 404 + " " + " File not found.");
					out.println("Content-type: text/html\n");
					out.println("404 File not found.");
				} else {
		    		byte content[] = new byte[(int) file.length()];
		    		new FileInputStream(file){{read(content);close();}};
		            out.println("HTTP/1.0 200 OK");
		            out.println("Content-length: " + content.length + "\n");
		            out.write(content);
				}
			} finally {
	            if (out != null)
	            	out.close();
	            if (in != null) 
	            	in.close();
			}
        } catch (RuntimeException | IOException e) {
        	System.out.println("Command " + cmdLine);
			e.printStackTrace();
		}
    }

	public static void main (String[] args) throws IOException {
		for (ServerSocket server = new ServerSocket(8000);;) {
            new Thread(new SimpleHttpServerApp(server.accept())).start();
        }
	}

}
