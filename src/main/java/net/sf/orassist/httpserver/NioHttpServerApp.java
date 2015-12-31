package net.sf.orassist.httpserver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class NioHttpServerApp {
	
	private String docRoot = "htdocs";

	private CharsetEncoder encoder = Charset.forName("utf-8").newEncoder();
	private CharsetDecoder decoder = Charset.forName("utf-8").newDecoder();

	public void start() throws Exception {
		
		Selector selector = Selector.open();
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		serverChannel.socket().setReuseAddress(true);
		serverChannel.socket().bind(new InetSocketAddress(8000));
		while(true) {
			while (selector.select() > 0) {
				for (SelectionKey key : selector.selectedKeys()) {
					if (key.isAcceptable()) {
						ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
						SocketChannel channel = ssc.accept();
						if (channel != null) {
							channel.configureBlocking(false);
							channel.register(selector, SelectionKey.OP_READ);
						} else {
							System.out.println ("SocketChannel is null after accept ServerSocketChannel.");
						}
					} else if (key.isReadable()) {
						SocketChannel channel = (SocketChannel) key.channel();
						channel.configureBlocking(false);
						StringBuilder builder = new StringBuilder();
						for (ByteBuffer buf = ByteBuffer.allocate(10240); channel.read(buf) > 0; ) { 
							buf.flip();
							builder.append(decoder.decode(buf));
							//buf.reset();
						}
						String receive = builder.toString();
						System.out.println(receive);
//						channel.register(selector, SelectionKey.OP_WRITE);
//					} else if (key.isWritable()) {
//						SocketChannel channel = (SocketChannel) key.channel();

						doResponse(channel, receive);
						channel.shutdownInput();
						channel.close();
					}
				}
			}
		}
	}

//	private String receive(SocketChannel socketChannel) throws Exception {
//		ByteBuffer buffer = ByteBuffer.allocate(1024);
//		byte[] bytes = null;
//		int size = 0;
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		while ((size = socketChannel.read(buffer)) > 0) {
//			buffer.flip();
//			bytes = new byte[size];
//			buffer.get(bytes);
//			baos.write(bytes);
//			buffer.clear();
//		}
//		bytes = baos.toByteArray();
//
//		return new String(bytes);
//	}

	void write(SocketChannel channel, String s) throws CharacterCodingException, IOException {
		channel.write(encoder.encode(CharBuffer.wrap(s)));	
	}

	private void doResponse(SocketChannel channel, String queryString) throws CharacterCodingException, IOException {
		
		StringBuilder ret = new StringBuilder();

        String[] cmd = queryString.split(" ");

        File file = new File(docRoot + (cmd[1].endsWith("/") ? cmd[1] + "index.html" : cmd[1]));
		if (!cmd[0].equals("GET") || !file.exists()) {
			ret.append("HTTP/1.0 " + 404 + " " + " File not found.\n");
			ret.append("Content-type: text/html\n\n");
			ret.append("404 File not found.\n");
            write(channel, ret.toString());
		} else {
            ret.append("HTTP/1.0 200 OK\n");
            ret.append("Content-length: " + file.length() + "\n\n");
            write(channel, ret.toString());
            
            RandomAccessFile raf = null;
            FileChannel fcin = null;
            try {
				raf = new RandomAccessFile(file, "r");
		        fcin = raf.getChannel();
		        ByteBuffer fileBuf = ByteBuffer.allocate(102400);
	    		
		        for (;fcin.read(fileBuf) > 0;) {
		        	fileBuf.flip();
		        	channel.write(fileBuf);
		        }
            
            } finally {
            	if (fcin != null) fcin.close(); 
            	if (raf != null) raf.close(); 
            }
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		new NioHttpServerApp().start();

	}
}