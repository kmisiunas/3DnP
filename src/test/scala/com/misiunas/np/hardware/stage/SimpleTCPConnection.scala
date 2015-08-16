package com.misiunas.np.hardware.stage

import java.io.{BufferedReader, DataOutputStream}
import java.net.Socket

/**
 * Created by kmisiunas on 15-08-15.
 */
class SimpleTCPConnection {
    //val port = Option(System.getenv("PORT")).map(_.toInt).getOrElse(9999)

    //ActorSystem().actorOf( Props(new TCPEchoServer(port)) )

   // val inFromUser: BufferedReader = new BufferedReader( new InputStreamReader(System.in));
    val clientSocket: Socket = new Socket("192.168.0.199", 50000);
  
    val outToServer: DataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
    val inFromServer: BufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));



    outToServer.writeBytes("CSV?" + '\n');

    var modifiedSentence: String = ""
    modifiedSentence = inFromServer.readLine();

    //inFromServer.ready()

    System.out.println("FROM SERVER: " + modifiedSentence);
    clientSocket.close();
}
