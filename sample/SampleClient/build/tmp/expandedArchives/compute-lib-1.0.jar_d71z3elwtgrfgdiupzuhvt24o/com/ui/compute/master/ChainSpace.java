package com.ui.compute.master;

import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Class ChainSpace
 * A data structure used to organise data streaming in from multiple sources (typically from multiple threads)
 * into one ordered stream. Data units can be put into the chain space using putObject(Object object). The
 * arriving units are then encapsulated into indexed nodes. The chain space contains a port, through which
 * organised nodes exit, and a tail, which exists to notify the port when all the nodes have been organised.
 * Upon entry, each node forms links with the node directly before it and the node directly after it in the
 * sequence if they are present in the chain space. We will refer to these as 'before-links' and 'after-links'
 * respectively.
 * 
 * The resulting groups of nodes are referred to as 'chains'. The port requests the next node in the sequence and
 * if it is present, it along with any nodes to which it is linked are sent out through the port. The method
 * getObjectBuffer(List buffer) acquires the next node or ordered chain of nodes available, blocking if necessary.
 * When the final unit is put into the chain space, it forms an after-link with the tail and as soon as the tail
 * is received by the port, getObjectBuffer(List buffer) returns true to signal that all the data has been
 * organised. The chain space can then be closed.
 * 
 * The main contribution of this class is to allow sequential data to be processed in different places (perhaps
 * on different machines in a distributed system) and then to sequence the incoming data, updating results as a
 * dynamic stream rather than waiting for all processing to finish before returning the result. This is a thread-
 * safe class and as such no additional synchronisation is needed when making put or get calls.
 * 
 * @author Usman Islam (2015)
 */
public class ChainSpace{

private Port mPort;
private Tail mTail;
private SynchronizedLBQ<Object> mBuffer;
private SynchronizedHashMap<Integer, Node> mNodes;
private boolean mIsOpen;
private boolean mPortIsLinked = false;
private boolean mNodesSent = false;
private static final int PORT_POSITION = -1;
private static final int TAIL_POSITION = -20;



/**
 * Construct a new chain space which takes data units of the specified class.
 * @param dataType the class of the data which will be processed by this chain space
 */
public ChainSpace(Class<?> dataType){
	
	mIsOpen = true;

	mPort = new Port(this, dataType);
	mTail = new Tail(this, dataType);
  
	synchronized(this){  
	  mBuffer = new SynchronizedLBQ<Object>();
		// Thread-safe version of HashMap to store nodes
	  mNodes = new SynchronizedHashMap<Integer, Node>();
	}
  
  	mNodes.put(PORT_POSITION, mPort);
  
}
  
  
	/**
	 * Put the next data unit into the chain space.
	 * @param position the position of this data unit in the sequence
	 * @param object the data unit to be added
	 * @param isEnd true if this data unit is the last one to be added
	 */
	public void putObject(int position, Object object, boolean isEnd){
		System.out.println("Greetings from putObject");
		if(mIsOpen){
		
		// Thread-safe unit of data
		Node node = new Node(position, object, this);
	
		if(isEnd){
			System.out.println("Linking to tail, position: " + position);
			node.linkToTail(mTail);
			System.out.println("Linked to tail, position: " + position);
		}
	  
		if(position == mPort.getRequestedLink()){
			System.out.println("position is requested link: " + mPort.getRequestedLink());
	    // Must be in this order! Otherwise the node won't be inserted into the HashMap
	    // before the main thread is notified that it is linked. This would result in
	    // a null pointer when it tries to access the node.
	    mNodes.put(position, node);
	    if(mNodes.containsKey(position + 1)){
			node.setAfterLink(mNodes.get(position + 1));
			}
	    node.linkToPort(mPort);
		} else{
			System.out.println("position is not requested link, RL: " + mPort.getRequestedLink());
				if(mNodes.containsKey(position + 1)){
				node.setAfterLink(mNodes.get(position + 1));
				}
				if(mNodes.containsKey(position - 1)){
				node.setBeforeLink(mNodes.get(position - 1));
				}
	      
	      		mNodes.put(position, node);
	  
			}
			
		} else{
			IllegalStateException e = new IllegalStateException();
			throw e;
		}
	
	}

  /**
   * Get the next node or ordered chain of nodes available from the chain space.
   * @param result the buffer which will be populated with the node(s)
   * @return true if all required nodes have been acquired after invocation
   */
  public boolean getObjectBuffer(List<Object> result) throws InterruptedException{
	  	  // Returns true if the chain space is empty after invocation.
          SynchronizedLBQ<Object> bq = new SynchronizedLBQ<Object>();
          System.out.println("man1");
          boolean done = mPort.getObjectChain(bq);
          System.out.println("man2");
          int size = bq.size();
          for(int i = 0; i < size; i++){
        	  result.add(bq.take());
          }
          System.out.println("man3");
          if(done){
        	  System.out.println("man True");
        	  return true;
          } else{
        	  System.out.println("man False");
        	  return false;
          }
      }
  
  /**
   * Returns all nodes currently in this chain space, including the tail.
   */
  synchronized SynchronizedHashMap<Integer, Node> getNodes(){
    return mNodes;
  }
  
  /**
   * Returns the number of nodes currently in this chain space, including the tail.
   */
  public synchronized int size(){
	  return mNodes.size();
  }
  
  /**
   * Returns the port for this chain space.
   */
  public synchronized Port getPort(){
	  return mPort;
  }
  
  /**
   * Returns true if the chain space is open.
   */
  public synchronized boolean isOpen(){
	  return mIsOpen;
  }
  
  /**
   * Closes the chain space.
   */
  public synchronized void close(){
	  mIsOpen = false;
	  notifyAll();
  }
  
  /**
   * Returns true if the port is currently linked to a particular node.
   */
  public synchronized boolean portIsLinked(){
	  return mPortIsLinked;
  }
  
  /**
   * Returns true if all the data has been sent out through the port.
   */
  public synchronized boolean nodesSent(){
	  return mNodesSent;
  }
  
  /**
   * Notifies the chain space that all the data has been sent out through the port.
   */
  public synchronized void notifyNodesSent(){
	  mNodesSent = true;
  }
  
  /**
   * Sets the linked state of the port.
   * @param isLinked whether or not the port is linked
   */
  public synchronized void setPortState(boolean isLinked){
	  mPortIsLinked = isLinked;
  }
  
  
   /**
    * Class Node
    * A thread-safe class to encapsulate a unit of data and its position in the sequence. Each node can be linked to two other nodes,
    * one on each side, by setting mAfterLink and mBeforeLink. setAfterLink(Node n) and setBeforeLink(Node n) should be used
    * to create a mutual link between the two nodes. When each next unit is ready to be sent out through the port, each transitively
    * after-linked node will be sent in the same transaction and the port will then wait for the next node or chain of nodes in the
    * sequence.
    */
   class Node{

     
     private int mPosition;
     private Object mObject;
     private ChainSpace mChainSpace;
     private Node mAfterLink = null;
     private Node mBeforeLink = null;
     private boolean mIsEnd = false;
     
     /**
      * Construct a copy of a specified node in the specified chain space.
      * 
      * @param original the node to make a copy of
      * @param chainSpace the chain space this node is in
      */
     public Node(Node original, ChainSpace chainSpace){
       this(original.getPosition(), original.getObject(), chainSpace);
     }
     
     /**
      * Construct a new node with the given position and data unit in the
      * specified chain space.
      * 
      * @param position the position of this data unit in the sequence
      * @param object the data unit to be encapsulated
      * @param chainSpace the chain space this node is in
      */
     public Node(int position, Object object, ChainSpace chainSpace){
       synchronized(this){
        mPosition = position;
        mObject = object;
        mChainSpace = chainSpace;
       }
     }
     
     /**
      * Mutually set the after-link of this node.
      * 
      * @param n the node to which this node will be after-linked
      */
     public synchronized void setAfterLink(Node n){
       if(mAfterLink != null){
       	mAfterLink.detachBeforeLink();
       }
       mAfterLink = n;
       n.simpleBeforeLink(this);
     }
     
     /**
      * Mutually set the before-link of this node.
      * 
      * @param n the node to which this node will be before-linked
      */
     public synchronized void setBeforeLink(Node n){
       if(mBeforeLink != null){
         mBeforeLink.detachAfterLink();
       } else{
    	   
       }
       mBeforeLink = n;
       if(n == null){
    	   System.out.println("n is null");
       }
       n.simpleAfterLink(this);
     }
     
     /**
      * Set the after-link of this node (the link will not be mutual).
      * This method is only used within setBeforeLink to ensure the link
      * is mutual.
      * 
      * @param n the node to set mAfterLink as
      */
     synchronized void simpleAfterLink(Node n){
       mAfterLink = n;
     }
     
     /**
      * Set the before-link of this node (the link will not be mutual).
      * This method is only used within setAfterLink to ensure the link
      * is mutual.
      * 
      * @param n the node to set mBeforeLink as
      */
     synchronized void simpleBeforeLink(Node n){
       mBeforeLink = n;
     }
     
     /**
      * Mutually detach this node's after-link.
      */
     public synchronized void detachAfterLink(){
       if(mAfterLink != null){
      	 mAfterLink.simpleBeforeDetach();
       }
       mAfterLink = null;
     }
     
     /**
      * Mutually detach this node's before-link.
      */
     public synchronized void detachBeforeLink(){
      if(mBeforeLink != null){
      	 mBeforeLink.simpleAfterDetach();
       }
       mBeforeLink = null;
     }
     
     /**
      * Detach this node's after-link (this will not be mutual).
      * This method is only used in detachBeforeLink to ensure
      * the detach is mutual.
      */
     synchronized void simpleAfterDetach(){
      mAfterLink = null; 
     }
     
     /**
      * Detach this node's before-link (this will not be mutual).
      * This method is only used in detachAfterLink to ensure
      * the detach is mutual.
      */
     synchronized void simpleBeforeDetach(){
      mBeforeLink = null; 
     }
     
     /**
      * Wait for the port to be detached from any other node and
      * then before-link this node to the port, indicating that it along
      * with the others in its chain are ordered and should be sent
      * out as a result buffer.
      * 
      * @param port the port of this node's chain space
      */
     public synchronized void linkToPort(Port port){
    	 
    	 // Wait for the port to handle its current link
    	 System.out.println("linkToPort1");
         while(mChainSpace.portIsLinked() && mIsOpen){
           	try{
           		System.out.println("linkToPort2");
           		wait();
           	} catch(InterruptedException e){
           		System.out.println(e.getMessage());
           	}
           }
         System.out.println("linkToPort3");
       if(mBeforeLink != null){
    	  mBeforeLink.simpleAfterDetach();
       }
       System.out.println("linkToPort4");
       mBeforeLink = port;
       port.setCurrentLink(this);
       System.out.println("linkToPort5");
     }
     
     /**
      * After-link this node to the tail. When the tail is received
      * by the port, this indicates that all data has been processed
      * and the chain space can be closed.
      */
     public synchronized void linkToTail(Tail tail){
       if(mAfterLink != null){
    	  mAfterLink.simpleBeforeDetach();
       }
       mAfterLink = tail;
       tail.setLink(this);
       mIsEnd = true;
     }
     
     /**
      * Detach this node from the tail.
      */
     public synchronized void detachFromTail(Tail tail){
       mAfterLink = null;
       tail.simpleDetach();
       mIsEnd = false;
     }
     
     /**
      * Returns the node to which this node is after-linked
      * or null if it isn't.
      */
     public synchronized Node getAfterLink(){
      return mAfterLink; 
     }
     
     /**
      * Returns the node to which this node is before-linked
      * or null if it isn't.
      */
     public synchronized Node getBeforeLink(){
      return mBeforeLink; 
     }
     
     /**
      * Set the position of this node.
      * 
      * @param the new position
      */
     public synchronized void setPosition(int position){
       mPosition = position;
     }
     
     /**
      * Set the data unit of this node.
      * 
      * @param the data unit to be encapsulated.
      */
     public synchronized void setObject(Object object){
      mObject = object; 
     }
     
     /**
      * Returns the position of this node.
      */
     public synchronized int getPosition(){
      return mPosition; 
     }
     
     /**
      * Returns the data unit encapsulated by this node.
      */
     public synchronized Object getObject(){
      return mObject; 
     }
    
     
   }
   
  /**
   * Class Port
   * A specialised node which takes care of sending nodes or organised chains
   * of nodes out as result buffers.
   */
  class Port extends Node{
     
     private ChainSpace mChainSpace;
     private Node mCurrentLink = null;
     private boolean mIsLinked = false;
     private int mRequestedLink = 0;
     
     
     
    /**
     * Contruct a new port in the specified chain space
     * for the given data type.
     * 
     * @param chainSpace the chain space of this port
     * @param the class of the data in this port's chain space
     */
     public Port(ChainSpace chainSpace, Class<?> dataType){
       super(PORT_POSITION, dataType, chainSpace);
       synchronized(this){
         mChainSpace = chainSpace;
       }
     }
     
     /**
      * Links the port to the given node if its position matches
      * the position requested by the port.
      * 
      * @param the node to try and link to
      */
     public synchronized void setCurrentLink(Node n){
      
      if(!mIsOpen){
    	  throw new IllegalStateException();
      } else{

       if(n != null && n.getPosition() == mRequestedLink){
       mCurrentLink = n;
       mIsLinked = true;
       mChainSpace.setPortState(mIsLinked);
       notifyAll();
       }
     }
     }
     
     /**
      * Set the new position requested by the port.
      * 
      * @param the position of the next node which will be linked to the port
      */
     public synchronized void setRequestedLink(int position){
      	mRequestedLink = position; 
     }
     
     /**
      * Returns the position of the node currently requested by the port.
      */
     public synchronized int getRequestedLink(){
      return mRequestedLink; 
     }
     
     /**
      * Returns true if the port is currently linked.
      */
     public synchronized boolean isLinked(){
      return mIsLinked; 
     }
     
     /**
      * Method to get the next node or chain of nodes available from the chain space.
      * This method is called by the chain space whenever a result buffer is requested
      * from outside. It waits until there is a result available.
      * 
      * @param buffer the queue to populate with the next node or chain in the sequence
      */
     public synchronized boolean getObjectChain(SynchronizedLBQ<Object> buffer) throws InterruptedException{
       
    	 // Returns true if the chain space is empty after invocation.
    	 
       if(mChainSpace.nodesSent()){
    	   return true;
       }
       mBuffer = buffer;
       SynchronizedHashMap<Integer, Node> nodes = mChainSpace.getNodes();
      		while(!mIsLinked && mIsOpen){
      			// Our synchronization lock is the Port
  				wait();
			}
      	if(!mIsOpen){
      		throw new IllegalStateException();
      	} else{
       
			processChain(mCurrentLink, nodes);
	       
	       if(mCurrentLink == null){
	        mIsLinked = false;
	        mChainSpace.setPortState(mIsLinked);
	        notifyAll();
	       } else{
	       }
       
      	}
       	
      	if(mChainSpace.nodesSent()){
     	   return true;
        } else{
        	return false;
        }
      	
     }
     
     /**
      * Utility method to go through the chain and add each node to the result buffer,
      * checking for the tail and notifying the chain space if it is received.
      */
     private void processChain(Node currentLink, SynchronizedHashMap<Integer, Node> nodes){
     
    	 try{
            mBuffer.put(currentLink.getObject());
       		int currentPosition = currentLink.getPosition();
       		nodes.remove(currentPosition);
       		currentLink = currentLink.getAfterLink();
       
       		while(currentLink != null){
       			if(currentLink instanceof ChainSpace.Tail){
       				((Tail) currentLink).getLink().detachFromTail((Tail) currentLink);
       				mChainSpace.notifyNodesSent();
       				currentLink = null;
       				break;
       			}
         		mBuffer.put(currentLink.getObject());
              	currentPosition = currentLink.getPosition();
              	nodes.remove(currentPosition);
              	currentLink = currentLink.getAfterLink();
       		}
       		
       		if(currentLink == null){
       			mCurrentLink = null;
       		} else{
       		}
       		
       		setRequestedLink(currentPosition + 1);
    	 } catch(InterruptedException e){
    		 System.out.println(e.getMessage());
    	 }
       		
       		
     
     }
     
  }
    
    
     /**
      * Class Tail
      * A specialised node which acts as a poison pill which is
      * after-linked to the final node in the sequence when it
      * arrives. When the tail is received by the port, the
      * caller is notified that all the nodes have been processed.
      */
    class Tail extends Node{
      
      private ChainSpace mChainSpace;
      private Node mLink = null;
      private boolean mIsLinked = false;
      
      
      /**
       * Constructs a new tail in the specified chain space for the
       * given data type.
       * 
       * @param chainSpace the chain space of this tail
       * @param dataType the class of data units handled by this tail's chain space
       */
      public Tail(ChainSpace chainSpace, Class<?> dataType){
        super(TAIL_POSITION, dataType, chainSpace);
        synchronized(this){
          mChainSpace = chainSpace;
        }
      }
      
      /**
       * Set the tail's link to the specified node.
       * 
       * @param the node to link to
       */
      public synchronized void setLink(Node n){
        if(n != null){ 
        mLink = n;
        mIsLinked = true;
        }
      }
      
      /**
       * Returns the current link of this tail, or null if there
       * is no link yet.
       */
      public synchronized Node getLink(){
       return mLink; 
      }
      
      /**
       * Returns true if the tail is currently linked.
       */
      public synchronized boolean isLinked(){
       return mIsLinked; 
      }
      
      /**
       * Does a one-way detach from any node currently
       * linked to this tail.
       */
      public synchronized void simpleDetach(){
       	mLink = null;
        mIsLinked = false;
      }
      
      
    }
    
    
    /**
     * A thread-safe version of HashMap.
     */
    class SynchronizedHashMap<K, V>{
    	private HashMap<K, V> mHashMap;
    	
    	public SynchronizedHashMap(){
    		mHashMap = new HashMap<K, V>();
    	}
    	
    	public synchronized void put(K key, V value){
    		mHashMap.put(key, value);
    	}
    	
    	public synchronized void remove(K key){
    		mHashMap.remove(key);
    	}
    	
    	public synchronized V get(K key){
    		V val = mHashMap.get(key);
    		return val;
    	}
    	
    	public synchronized boolean containsKey(K key){
    		return mHashMap.containsKey(key);
    	}
    	
    	public synchronized Collection<V> values(){
    		return mHashMap.values();
    	}
    	
    	public synchronized int size(){
    		return mHashMap.size();
    	}
    }
    
    /**
     * A thread-safe version of LinkedBlockingQueue.
     */
    private class SynchronizedLBQ<T>{

    	private LinkedBlockingQueue<T> mQueue;
    	
    	public SynchronizedLBQ(){
    		synchronized(this){
    		mQueue = new LinkedBlockingQueue<T>();
    		}
    	}
    	
    	public synchronized void put(T object) throws InterruptedException{
    		mQueue.put(object);
    	}
    	
    	public synchronized T take() throws InterruptedException {
    		return mQueue.take();
    	}
    	
    	public synchronized T[] toArray(){
    		return (T[]) mQueue.toArray();
    	}
    	
    	public synchronized int size(){
    		return mQueue.size();
    	}
    	
    }
    

}
