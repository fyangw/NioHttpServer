package net.sf.orassist.httpserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;

public class SimpleHttpServerApp implements Consumer<Socket> {

	private static final Logger log = LoggerFactory.getLogger(SimpleHttpServerApp.class);

	private static String docRoot = "htdocs";
	
	public void accept(Socket client) {
		procClient(client);
	}
	
	public static void procClient(Socket client) {
		log.debug("request accept socket {}", client);

		BufferedReader in = null;
		DataOutputStream out = null;
		String firstLine = null;
		String cmd[] = null;
		int httpCode;
        try {
        	try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream(), "utf-8"));
	            out = new DataOutputStream(client.getOutputStream());
	
	            if ((firstLine = in.readLine()) == null) {
	    			httpCode = output404Response(out, cmd, in, "recieved a null request");
	            } else if ((cmd = firstLine.split(" ")).length != 3) {
	    			httpCode = output404Response(out, cmd, in, "cannot split request string first line");
	            } else {
	    	        switch (cmd[0]) {
	    	        case "GET":
		    			httpCode = outputGetResponse(out, cmd, in);
	    	        	break;
	    	        case "POST":
		    			httpCode = outputPostResponse(out, cmd, in);
	    	        	break;
	    	        default:
		    			httpCode = output501Response(out, cmd, in, "unsupport method");
	    	        }
	            }
	    		log.info("output response {} to {} from request {}", httpCode, client, firstLine);

			} finally {
	            if (out != null)
	            	out.close();
	            if (in != null) 
	            	in.close();
			}
        } catch (RuntimeException | IOException e) {
			log.error("unknown error occured when processing " + firstLine, e);
		}
    }

	private static int outputPostResponse(DataOutputStream out, String[] cmd, BufferedReader in) throws IOException {
		int httpCode = output404Response(out, cmd, in, "post is not supported");
		return httpCode;
	}

	private static int outputGetResponse(DataOutputStream out, String[] cmd, BufferedReader in) throws IOException {
		int httpCode;
		String filename = docRoot + (cmd[1].endsWith("/") ? cmd[1] + "index.html" : cmd[1]);
		File file = new File(filename);

		if (file.getAbsolutePath().length() < new File(docRoot).length()) {
			httpCode = output404Response(out, cmd, in, "attack detected");
		} else if (!file.exists()) {
			httpCode = output404Response(out, cmd, in, "cannot find file");
		} else {
            httpCode = 200;
            String header = "HTTP/1.0 " + httpCode + " OK\n" 
            				+ "Content-length: " + file.length() + "\n\n";
			log.debug("output response header: {}", header.replace("\n", "\\n"));
            out.writeUTF(header);
            out.flush();
            
    		byte buf[] = new byte[1024];
    		new FileInputStream(file){{
    			try{for (int len = 0; (len = read(buf)) > 0; out.write(buf, 0, len));}
    			finally{close();}
    		}};

		}
		
		return httpCode;
	}

	private static int output404Response(DataOutputStream out, String[] cmd, BufferedReader in, String reason) throws IOException {
		int httpCode = 404;
    	log.debug("output " + httpCode + " response, {} {}", reason, cmd);
        out.writeUTF("HTTP/1.0 " + httpCode + " " + " File not found.\n"
        		+ "Content-type: text/html\n\n"
        		+ httpCode + " File not found.\n");
        return httpCode;
	}

	private static int output501Response(DataOutputStream out, String[] cmd, BufferedReader in, String reason) throws IOException {
		int httpCode = 501;
    	log.debug("output " + httpCode + " response, {} {}", reason, cmd);
        out.writeUTF("HTTP/1.0 " + httpCode + " " + " not implemented.\n"
        		+ "Content-type: text/html\n\n"
        		+ httpCode + " not implemented.\n");
        return httpCode;
	}

	static void listen(int port) throws IOException, InterruptedException, InstantiationException, IllegalAccessException {
    	log.info("start listening to {}", port);
		WorkerPool<Socket> pool = new WorkerPool<>(1, SimpleHttpServerApp.class);
		for (ServerSocket server = new ServerSocket(port);;) {
			Socket client = server.accept();
			pool.setWorkload(client);
	        //new Thread(()->{SimpleHttpServerApp.procClient(c);}).start();
	    }
	}

	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException {
		SimpleHttpServerApp.listen(8000);
	}

}
