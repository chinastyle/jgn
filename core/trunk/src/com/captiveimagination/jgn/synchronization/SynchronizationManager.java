/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Aug 26, 2007
 */
package com.captiveimagination.jgn.synchronization;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.captiveimagination.jgn.MessageClient;
import com.captiveimagination.jgn.Updatable;
import com.captiveimagination.jgn.clientserver.JGNClient;
import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.captiveimagination.jgn.event.ConnectionListener;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.ro.RemoteObjectManager;
import com.captiveimagination.jgn.synchronization.message.SynchronizeCreateMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeRemoveMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeRequestIDMessage;

/**
 * Instantiate for each synchronization session you want to maintain.
 * 
 * @author Matthew D. Hicks
 */
public class SynchronizationManager implements Updatable, MessageListener, JGNConnectionListener {
	private JGNServer server;
	private JGNClient client;
	
	private Queue<Short> used;				// Only used by server
	private Queue<SyncWrapper> idQueue;		// Client only - when waiting for a syncObjectId
	
	private GraphicalController controller;
	
	private Queue<SyncWrapper> queue;
	private Queue<SyncWrapper> disabled;
	private Queue<SyncWrapper> passive;
	
	private Queue<SynchronizeCreateMessage> createQueue;
	private Queue<SynchronizeRemoveMessage> removeQueue;
	
	private Queue<SyncObjectManager> objectManagers;
	
	private boolean keepAlive;
	
	public SynchronizationManager(JGNServer server, GraphicalController controller) {
		this.server = server;
		this.controller = controller;
		server.addMessageListener(this);
		server.addClientConnectionListener(this);
		
		used = new ConcurrentLinkedQueue<Short>();
		
		init();
	}
	
	public SynchronizationManager(JGNClient client, GraphicalController controller) {
		this.client = client;
		this.controller = controller;
		client.addMessageListener(this);
		
		idQueue = new ConcurrentLinkedQueue<SyncWrapper>();
		
		init();
	}
	
	private void init() {
		keepAlive = true;
		queue = new ConcurrentLinkedQueue<SyncWrapper>();
		disabled = new ConcurrentLinkedQueue<SyncWrapper>();
		passive = new ConcurrentLinkedQueue<SyncWrapper>();
		objectManagers = new ConcurrentLinkedQueue<SyncObjectManager>();
		
		createQueue = new ConcurrentLinkedQueue<SynchronizeCreateMessage>();
		removeQueue = new ConcurrentLinkedQueue<SynchronizeRemoveMessage>();
	}
	
	/**
	 * Register an object authoritative from this peer.
	 * 
	 * @param object
	 * @param createMessage
	 * @param updateRate
	 * @throws IOException 
	 */
	public void register(Object object, SynchronizeCreateMessage createMessage, long updateRate) throws IOException {
		// Get player id
		short playerId = 0;
		if (client != null) {
			playerId = client.getPlayerId();
		}
		
		// Create SyncWrapper
		SyncWrapper wrapper = new SyncWrapper(object, updateRate, createMessage, playerId);
		if (server != null) {		// Server registering - we have the ids
			short id = serverNextId();
			wrapper.setId(id);
			wrapperFinished(wrapper);
		} else {					// Client registering - ask the server for an id
			// Create a request message for an id
			SynchronizeRequestIDMessage request = new SynchronizeRequestIDMessage();
			request.setRequestType(SynchronizeRequestIDMessage.REQUEST_ID);
			long id = client.sendToServer(request);
			wrapper.setWaitingId(id);
			
			// Add it to the waiting queue
			idQueue.add(wrapper);
		}
	}
	
	private void wrapperFinished(SyncWrapper wrapper) {
		// Set object id
		wrapper.getCreateMessage().setSyncObjectId(wrapper.getId());
		
		// Send create message onward
		if (client != null) {
			client.broadcast(wrapper.getCreateMessage());
		} else {
			server.sendToAll(wrapper.getCreateMessage());
		}
		
		// Add to manager to be updated
		queue.add(wrapper);
	}
	
	/**
	 * Unregister an object and remove from remote clients
	 * 
	 * @param object
	 * @return
	 * 		true if removed
	 */
	public boolean unregister(Object object) {
		// Find SyncWrapper
		SyncWrapper wrapper = findWrapper(object);
		if (wrapper == null) return false;
		
		// Remove remotely
		SynchronizeRemoveMessage remove = new SynchronizeRemoveMessage();
		remove.setSyncObjectId(wrapper.getId());
		if (client != null) {
			client.broadcast(remove);
			
			// Release id
			SynchronizeRequestIDMessage release = new SynchronizeRequestIDMessage();
			release.setRequestType(SynchronizeRequestIDMessage.RELEASE_ID);
			release.setSyncObjectId(wrapper.getId());
			client.sendToServer(release);
		} else {
			server.sendToAll(remove);
			
			// Release id
			serverReleaseId(wrapper.getId());
		}
		
		// Remove from self
		if (queue.remove(wrapper)) {
			return true;
		}
		return disabled.remove(wrapper);
	}
	
	/**
	 * Re-enable a disabled object.
	 * 
	 * @param object
	 */
	public void enable(Object object) {
		SyncWrapper wrapper = findWrapper(object);
		disabled.remove(wrapper);
		queue.add(wrapper);
	}
	
	/**
	 * Stop sending updates for a sync object.
	 * 
	 * @param object
	 */
	public void disable(Object object) {
		SyncWrapper wrapper = findWrapper(object);
		queue.remove(wrapper);
		disabled.add(wrapper);
	}
	
	private final synchronized short serverNextId() {
		short id = (short)1;
		while (used.contains(id)) {
			id++;
		}
		used.add(id);
		return id;
	}
	
	private final void serverReleaseId(short id) {
		used.remove(id);
	}
	
	private SyncWrapper findWrapper(Object object) {
		SyncWrapper wrapper = null;
		for (SyncWrapper sync : queue) {
			if (sync.getObject() == object) {
				wrapper = sync;
				break;
			}
		}
		if (wrapper == null) {
			for (SyncWrapper sync : disabled) {
				if (sync.getObject() == object) {
					wrapper = sync;
					break;
				}
			}
		}
		if (wrapper == null) {
			for (SyncWrapper sync : passive) {
				if (sync.getObject() == object) {
					wrapper = sync;
					break;
				}
			}
		}
		return wrapper;
	}
	
	private SyncWrapper findWrapper(short syncObjectId) {
		SyncWrapper wrapper = null;
		for (SyncWrapper sync : queue) {
			if (sync.getId() == syncObjectId) {
				wrapper = sync;
				break;
			}
		}
		if (wrapper == null) {
			for (SyncWrapper sync : disabled) {
				if (sync.getId() == syncObjectId) {
					wrapper = sync;
					break;
				}
			}
		}
		if (wrapper == null) {
			for (SyncWrapper sync : passive) {
				if (sync.getId() == syncObjectId) {
					wrapper = sync;
					break;
				}
			}
		}
		return wrapper;
	}
	
	public void addSyncObjectManager(SyncObjectManager som) {
		objectManagers.add(som);
	}
	
	public boolean removeSyncObjectManager(SyncObjectManager som) {
		return objectManagers.remove(som);
	}
	
	/**
	 * Called internally when a SynchronizeCreateMessage is received
	 * 
	 * @param message
	 */
	public Object create(SynchronizeCreateMessage message) {
		for (SyncObjectManager manager : objectManagers) {
			Object obj = manager.create(message);
			if (obj != null) {
				// Create SyncWrapper
				SyncWrapper wrapper = new SyncWrapper(obj, 0, message, message.getPlayerId());
				wrapper.setId(message.getSyncObjectId());
				passive.add(wrapper);
				return obj;
			}
		}
		return null;
	}
	
	/**
	 * Called internally when a SynchronizeRemoveMessage is received
	 * 
	 * @param message
	 */
	public Object remove(SynchronizeRemoveMessage message) {
		SyncWrapper wrapper = findWrapper(message.getSyncObjectId());
		for (SyncObjectManager manager : objectManagers) {
			if (manager.remove(message, wrapper.getObject())) {
				unregister(wrapper.getObject());
				return true;
			}
		}
		return false;
	}

	public boolean isAlive() {
		return keepAlive;
	}

	public void update() throws Exception {
		// Create objects
		SynchronizeCreateMessage createMessage;
		while ((createMessage = createQueue.poll()) != null) {
			create(createMessage);
		}

		// Remove objects
		SynchronizeRemoveMessage removeMessage;
		while ((removeMessage = removeQueue.poll()) != null) {
			remove(removeMessage);
		}
		
		for (SyncWrapper sync : queue) {
			if (server != null) {
				sync.update(server, controller);
			} else {
				sync.update(client, controller);
			}
		}
	}
	
	public void shutdown() {
		keepAlive = false;
	}

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}

	public void messageReceived(Message message) {
		if (message instanceof SynchronizeCreateMessage) {
			createQueue.add((SynchronizeCreateMessage)message);
		} else if (message instanceof SynchronizeRemoveMessage) {
			removeQueue.add((SynchronizeRemoveMessage)message);
		} else if (message instanceof SynchronizeMessage) {
			SynchronizeMessage m = (SynchronizeMessage)message;
			SyncWrapper wrapper = findWrapper(m.getSyncObjectId());
			if (wrapper == null) {
				System.err.println("Unable to find object: " + m.getSyncObjectId() + " on " + (server != null ? "Server" : "Client"));
				return;
			}
			Object obj = wrapper.getObject();
			if (controller.validateMessage(m, obj)) {
				// Successfully validated synchronization message
				controller.applySynchronizationMessage(m, obj);
			} else {
				// Failed validation, so we ignore the message and send back our own
				m = controller.createSynchronizationMessage(obj);
				message.getMessageClient().sendMessage(m);
			}
		} else if (message instanceof SynchronizeRequestIDMessage) {
			SynchronizeRequestIDMessage request = (SynchronizeRequestIDMessage)message;
			if (request.getRequestType() == SynchronizeRequestIDMessage.REQUEST_ID) {
				short id = serverNextId();
				request.setRequestType(SynchronizeRequestIDMessage.RESPONSE_ID);
				request.setSyncObjectId(id);
				request.getMessageClient().sendMessage(request);
			} else if (request.getRequestType() == SynchronizeRequestIDMessage.RESPONSE_ID) {
				for (SyncWrapper wrapper : idQueue) {
					if (wrapper.getWaitingId() == request.getId()) {
						wrapper.setId(request.getSyncObjectId());
						wrapperFinished(wrapper);
						idQueue.remove(wrapper);
						break;
					}
				}
			} else if (request.getRequestType() == SynchronizeRequestIDMessage.RESPONSE_ID) {
				serverReleaseId(request.getSyncObjectId());
			}
		}
	}

	public void messageSent(Message message) {
	}

	public void connected(JGNConnection connection) {
		// Client connected to server - send creation messages to new connection
		System.out.println("**************** CONNECTED!");
		for (SyncWrapper wrapper : queue) {
			connection.sendMessage(wrapper.getCreateMessage());
		}
		for (SyncWrapper wrapper : disabled) {
			connection.sendMessage(wrapper.getCreateMessage());
		}
		for (SyncWrapper wrapper : passive) {
			connection.sendMessage(wrapper.getCreateMessage());
		}
	}

	public void disconnected(JGNConnection connection) {
		// Remove all connections associated with this player
		short playerId = connection.getPlayerId();
		for (SyncWrapper wrapper : passive) {
			if (wrapper.getOwnerPlayerId() == playerId) {
				SynchronizeRemoveMessage removeMessage = new SynchronizeRemoveMessage();
				removeMessage.setSyncObjectId(wrapper.getId());
				removeQueue.add(removeMessage);
			}
		}
	}
}