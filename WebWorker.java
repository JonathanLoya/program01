/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

//import java.io.File;
import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;


public class WebWorker implements Runnable
{

private Socket socket;
private String webPath;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is); 
      
      File webFile = new File(webPath);     
      //System.out.println("<debug> "+webPath);
      String webPathExt = getFileExtension(webFile);
      //System.out.println("<debug> "+webPathExt);
      String headerType = "";
      
		if( webPathExt.equals("html") )
			headerType = "text/html";
      else if( webPathExt.equals("jpg") )
      	headerType = "image/jpg";
      else if( webPathExt.equals("png") )
      	headerType = "image/png";
      else if( webPathExt.equals("gif") )
      	headerType = "image/gif";
      else if( webPathExt.equals("ico") )
      	headerType = "image/x-icon";
     	
     	//System.out.println("<debug> "+headerType);
      writeHTTPHeader(os,headerType);
      if( webPathExt.equals("html") )
      	writeStringContent(os);
      else if( headerType.startsWith("image") )
      	writeImageContent(os,webPathExt);
      
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private void readHTTPRequest(InputStream is)
{
   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
		try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
		   
			if( line.startsWith("GET") )
			{
				System.out.println("<debug> "+line);
				//System.err.println("<debug> Found get request!");
				webPath = line.substring(line.indexOf(" ")+2,line.lastIndexOf(" "));
				//System.err.println("<debug> "+webPath);
			}
			 
		   if (line.length()==0) break;
		} catch (Exception e) {
	     System.err.println("Request error: "+e);
	     break;
		}
	}
	return;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   File webFile = new File(webPath);
   if( webFile.exists() && !webFile.isDirectory() )
   {
	   os.write("HTTP/1.1 200 OK\n".getBytes());
	   os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		//os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		//os.write("Content-Length: 438\n".getBytes()); 
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   } else
   {
	   os.write("HTTP/1.1 404 Not Found\n".getBytes());
	   os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		//os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		//os.write("Content-Length: 438\n".getBytes()); 
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		
		os.write("<html><head></head><body>\n".getBytes());
	   os.write("<h3>404 Not Found</h3>\n".getBytes());
	   os.write("</body></html>\n".getBytes());
   }
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeStringContent(OutputStream os) throws Exception
{
  FileReader fileReader = new FileReader(webPath);
  String fileContents = "";
  int i;
  while((i =  fileReader.read())!=-1)
  {
		char ch = (char)i;
		fileContents = fileContents + ch; 
  }
  //date variables
  Date d = new Date();
  DateFormat df = DateFormat.getDateTimeInstance();
  df.setTimeZone(TimeZone.getTimeZone("MST"));
  
  fileContents = fileContents.replaceAll("<cs371date>", df.format(d));
  fileContents = fileContents.replaceAll("<cs371server>", "Server: Jon's very own server");
  
  os.write(fileContents.getBytes());
/*
   os.write("<html><head></head><body>\n".getBytes());
   os.write("<h3>My web server works!</h3>\n".getBytes());
   os.write("</body></html>\n".getBytes());
*/
}

private void writeImageContent(OutputStream os, String imageExt) throws Exception
{
	//System.out.println("<debug> hecking image");
	System.out.println("filePath: "+webPath);
	byte[] imageInByte;
	BufferedImage originalImage = ImageIO.read(new File(webPath));

	// convert BufferedImage to byte array
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	String encodedImage = DatatypeConverter.printBase64Binary(baos.toByteArray());
	//String encodedImage = Base64.encode(baos.toByteArray());
	System.out.println("extension:"+imageExt);
	ImageIO.write(originalImage, imageExt, baos);
	baos.flush();
	imageInByte = baos.toByteArray();

	os.write(imageInByte);

	baos.close();
}

private static String getFileExtension(File file)
{
	String fileName = file.getName();
	if( fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0 )
		return fileName.substring( fileName.lastIndexOf(".")+1 );
	else return "";
}

} // end class
