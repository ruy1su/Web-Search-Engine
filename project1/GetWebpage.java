import java.net.*;
import java.io.*;

public class GetWebpage {
  public static void main(String args[]) throws Exception {

      // args[0] has the URL passed as the command parameter.
      // You need to retrieve the webpage corresponding to the URL and print it out on console
      // Here, we simply printout the URL
      	// System.out.println(args[0]);
      	String line;

      	try{
	      // Init a url object and establish a connection
	      URL url = new URL(args[0]);
	      URLConnection urlConnection = url.openConnection();

	      // Init a bufferedreader for reading strem
	      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

	      // read from the urlconnection via the bufferedreader
	      while ((line = bufferedReader.readLine()) != null)
	      {
	        System.out.println(line);
	      }
	      bufferedReader.close();
    	}
    	catch(Exception e)
    	{
      		e.printStackTrace();
    	}	
    }
}
