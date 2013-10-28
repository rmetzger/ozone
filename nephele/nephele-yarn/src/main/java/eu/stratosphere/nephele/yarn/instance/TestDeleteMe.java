package eu.stratosphere.nephele.yarn.instance;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Enumeration;

public class TestDeleteMe {

	/**
	 * The states of address detection mechanism.
	 * There is only a state transition if the current
	 * state failed to determine the address.
	 */
	enum AddressDetectionState {
		ADDRESS(50), 		//detect own IP based on the JobManagers IP address. Look for common prefix
		FAST_CONNECT(50),	//try to connect to the JobManager on all Interfaces and all their addresses.
							//this state uses a low timeout (say 50 ms) for fast detection.
		SLOW_CONNECT(1000);	//same as FAST_CONNECT, but with a timeout of 1000 ms (1s).
		
		
		private int timeout;
		AddressDetectionState(int timeout) {
			this.timeout = timeout;
		}
		public int getTimeout() {
			return timeout;
		}
	}
	
	public static void main(String[] args) throws IOException {
		AddressDetectionState strategy = AddressDetectionState.ADDRESS;
		
		InetSocketAddress jobManager = new InetSocketAddress(InetAddress.getByName("192.168.1.2"), 80);
		
		while(true) {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		    while (e.hasMoreElements())  {
		        NetworkInterface n = e.nextElement();
		        System.err.println("Interface:"+n.getName());
	        	Enumeration<InetAddress> ee = n.getInetAddresses();
		        while (ee.hasMoreElements()) {
		            InetAddress i = ee.nextElement();
		            System.err.println("Current strategy "+strategy);
		            switch(strategy) {
		            	case ADDRESS:
		            		if(hasCommonPrefix(jobManager.getAddress().getAddress(),i.getAddress())) {
		            			System.err.println("\t trying on address "+i.getHostAddress());
		            			if(tryToConnect(i, jobManager, strategy.getTimeout())) {
		            				System.err.println("+++ my own address seems to be "+i.getHostAddress()+"++++");
		            			}
		            		}
		            		break;
		            	case FAST_CONNECT:
		            	case SLOW_CONNECT:
		            		System.err.println("\t trying on "+strategy+" "+i.getHostAddress());
				            boolean correct = tryToConnect(i, jobManager, strategy.getTimeout());
				            System.err.println("Connectable "+correct);
				            if(correct) {
				            	System.err.println("+++ my own address seems to be "+i.getHostAddress()+"++++");
				            }
				            break;
		            }
		            
		        }
		    }
		    // state control
		    switch(strategy) {
			    case ADDRESS:
			    	strategy = AddressDetectionState.FAST_CONNECT;
			    	break;
			    case FAST_CONNECT:
			    	strategy = AddressDetectionState.SLOW_CONNECT;
			    	break;
			    case SLOW_CONNECT:
			    	throw new RuntimeException("The TaskManager failed to detect its own IP address");
		    }
		}
	}

	/**
	 * Checks if two addresses have a common prefix (first 2 bytes).
	 * Example: 192.168.???.???
	 * Works also with ipv6, but accepts probably too many addresses
	 */
	private static boolean hasCommonPrefix(byte[] address, byte[] address2) {
		return address[0] == address2[0] && address[1] == address2[1];
	}

	public static boolean tryToConnect(InetAddress fromAddress, SocketAddress toSocket, int timeout) throws IOException {
		boolean connectable = true;
        Socket socket = null;
        try {
        	socket = new Socket(); 
        	SocketAddress bindP = new InetSocketAddress(fromAddress, 0); // 0 = let the OS choose the port on this machine
			socket.bind(bindP);
        	socket.connect(toSocket,timeout);
        } catch(Exception ex) {
        	System.err.println("Cause: "+ex.getMessage());
        	connectable = false;
        } finally {
        	if(socket != null) {
        		socket.close();
        	}
        }
        return connectable;
	}
}
