import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class BattleshipClient {
	
	//Networking Variables
	Socket server;
	DataOutputStream dos = null;
	DataInputStream dis = null;
	InetAddress sIP = null;
	
	//Board Variables
	int row = 15;
	int col = 15;
	int board[];
	
	//Boolean to remain true while the game is in progress
	boolean inProgress = true;
	
	//Scanner to get user input
	Scanner scan;
	
	//Enum for GameStatus
	public enum GameStatus{
		
		CONTINUE(10),
		CLIENT_WON(20),
		CLIENT_LOST(30),
		ILLEGAL_MOVE(40);
		
		final int id;
		GameStatus(int id){
			this.id = id;
		}
		
	}
	
	//Enum for MoveStatus
	public enum MoveStatus{
		
		MISS(10),
		HIT(20),
		SINK(30),
		ILLEGAL_MOVE(40);
		
		final int id;
		MoveStatus(int id){
			this.id = id;
		}
		
	}
	
	/**
	 * Constructor for the BattleShipClient, contains the Game Loop.
	 */
	public BattleshipClient(){
		
		//Scanner is initialized in getServerIp
		getServerIP();
		establishConnection();
		createStreams();
		initBoard();
		
		printBoard();
		//Game Loop methods
		while(inProgress){
			getTurn();
		}
		
		closeStreams();
	}
	
	/****************Game Loop Methods Below ******************/
	
	/**
	 * Prints the board below, slightly different than what we had before, I wanted to 
	 * give the user an easier way to identify the rows, and make the board a lot easier to see.
	 */
	private void printBoard(){
		
		for(int y = 0; y < col+1; y++){
			for(int x = 0; x < row+1; x++){
				if(x == 0 && y == 0){
					System.out.print("#");
				}
				else if(x > 0 && x < row+1 && y == 0){
					System.out.print(x-1);
				}
				else if(y > 0 && y < col+1 && x == 0){
					System.out.print(y-1);
				}
				else if(board[row*(x-1) + (y-1)] == 0){
					System.out.print("_");
				}
				else if(board[row*(x-1) + (y-1)] == 1){
					System.out.print("H");
				}
				else if(board[row*(x-1) + (y-1)] == 2){
					System.out.print("M");
				}
				
				System.out.print("|\t");
			}
			System.out.println();
		}
		
	}
	
	/**
	 * Simulate a turn for the user, get a guess and then print out the board
	 */
	private void getTurn(){
		System.out.println("‘M’ indicates “miss.” ‘H’ indicates “hit.” " +
				"Enter your Move (enter negative number to quit)");
		
		System.out.print("\nSelected row: ");
		int t_row = scan.nextInt();
		System.out.print("Selected Column: ");
		int t_col = scan.nextInt();
		System.out.println();
		
		//If either number is negative, quit
		if(t_row < 0 || t_col < 0)
			quitGame();
		
		//If number was positive, send guess and get response
		sendGuess(t_row, t_col);
		getResponse(t_row, t_col);
	}
	
	/**
	 * Send the users guess to the server
	 * @param x : X coordinate
	 * @param y : Y coordinate
	 */
	private void sendGuess(int x, int y){
		
		try{
			dos.writeInt(row*x+y);
		}catch(IOException ex){
			System.err.println("Error sending your guess to the server");
			ex.printStackTrace();
		}
	}
	
	/**
	 * Get the response from the server based on the user guess
	 * @param t_row : The row the user guessed
	 * @param t_col : The col the user guessed
	 */
	private void getResponse(int t_row, int t_col){
		int moveStatus = 0;
		int gameStatus = 0;
		
		try{
			moveStatus = dis.readInt();
			gameStatus = dis.readInt();
		}catch(IOException ex){
			System.err.println("Error getting status from the server!");
			ex.printStackTrace();
		}
		
		//Determine what the response from the server was for the move
		switch(moveStatus){
		case 10: 
			board[row*t_row + t_col] = 2;
			printBoard();
			System.out.println("Miss!");
			break;
		case 20:
			board[row*t_row + t_col] = 1;
			printBoard();
			System.out.println("Hit!");
			break;
		case 30:
			board[row*t_row + t_col] = 1;
			printBoard();
			System.out.println("Sink!");
			break;
		case 40:
			printBoard();
			System.out.println("Illegal Move, Exiting!");
			break;
		}
		
		
		//Determine what the response was from the server for the Game Status
		switch(gameStatus){
		case 10:
			//Continue, do nothing
			break;
		case 20:
			//Game over, Player won
			System.out.println("You have Won the Game! Exiting!");
			inProgress = false;
			break;
		case 30:
			//Game over, Player lost
			System.out.println("You have Lost the Game! Exiting!");
			inProgress = false;
			break;
		case 40:
			//Illegal Move, you dun goofed
			inProgress = false;
			break;
		}
	}
	
	/**
	 * Display message to user and exit the game
	 */
	private void quitGame(){
		System.out.println("Thank you for playing, Come again!");
		inProgress = false;
		
		
		//Message the server letting it know you have quit the game!
		try{
			dos.writeInt(-1);
		}catch(IOException ex){
			System.err.println("Error messaging the server to quit");
			ex.printStackTrace();
		}
	}
	
	
	/**************** Board Methods Below *********************/
	
	/**
	 * Initialize the board, should be done for every new game
	 */
	private void initBoard(){
		board = new int[row*col];
	}
	
	
	/********************** Networking Methods Below *********************/
	
	/**
	 * Close all the streams: Socket, DataOut, DataIn, and also the Scanner
	 */
	private void closeStreams(){
		try{
			dos.close();
			dis.close();
			server.close();
			scan.close();
		}catch(IOException ex){
			System.err.println("Error closing your streams");
			ex.printStackTrace();
		}
	}
	
	/**
	 * Create the input/output streams with the server
	 */
	private void createStreams(){
		
		try {
			dos = new DataOutputStream(server.getOutputStream());
		} catch (IOException e1) {
			System.err.println("Error creating Output Stream");
			e1.printStackTrace();
		}
		
		
		try {
			dis = new DataInputStream(server.getInputStream());
		} catch (IOException e) {
			System.err.println("Error creating Input Stream");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Create the socket to connect with the server
	 */
	private void establishConnection(){
		
		try {
			server = new Socket(sIP, BattleshipServer.PORT);
		} catch (IOException e) {
			System.err.println("Error Creating Socket for the Server");
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the server IP from the user, and then use set that to the member variable sIP
	 */
	private void getServerIP(){
		
		scan = new Scanner(System.in);
		
		System.out.print("Please enter the server's IP address: ");
		
		try {
			sIP = InetAddress.getByName(scan.next()+ "");
		} catch (UnknownHostException e) {
			System.err.println("Could not resolve the Inet Address");
			e.printStackTrace();
		}
		
		System.out.println();
	}

	/**
	 * Main method for the class, creates new object
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new BattleshipClient();
	}

}
