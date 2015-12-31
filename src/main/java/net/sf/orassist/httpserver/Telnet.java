package net.sf.orassist.httpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Telnet implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Telnet.class);
	
	private static BufferedReader reader;

	public Telnet(BufferedReader reader) {
		this.reader = reader;
	}

	public static void main(String[] args) throws IOException {
		Socket sock = new Socket(args[0], Integer.parseInt(args[1]));
		logger.info("connected");
		sock.setSoTimeout(1000);
		
		new Thread(new Telnet(new BufferedReader(new InputStreamReader(sock.getInputStream(), "utf-8")))).start();
		PrintStream writer = new PrintStream(sock.getOutputStream());
		BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in, "gbk"));
		
		writer.println("GET / HTTP/1.0\n");
		writer.flush();
		
		logger.info("waiting for console input");
		for (String s;(s = consoleReader.readLine()) != null;) {
			logger.info("one line is read from console: {}", s);
			for (char c:s.toCharArray()) {
				System.out.print(c + " " + String.format("%02X", (byte)c) + " ");
			}
			System.out.println();
			writer.println(s + "\n\n");
			writer.flush();
			logger.info("one line is wroten from console: {}", s);
		}
	}

	@Override
	public void run() {
		try {
			logger.info("waiting for sock input");
			for (String s;(s = reader.readLine()) != null;) {
				logger.info("one line is read from socket: {}", s);
				System.out.println(s);
			}
			System.out.flush();
			System.exit(0);
		} catch (IOException e) {
			new RuntimeException(e);
		}
	}

}
