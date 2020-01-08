package com.mulesoft.support.objectstore;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.util.store.AbstractMonitoredObjectStore;

public class CustomMonitoredObjectStore<T extends Serializable> extends AbstractMonitoredObjectStore<T> {
	private static final Logger logger = Logger.getLogger(CustomMonitoredObjectStore.class);

	public ListableObjectStore localObjectStore = null;

	public CustomMonitoredObjectStore()

	{
		logger.info("Created new " + this.getClass().getName() + " as =" + this);
	}

	private CustomMonitoredObjectStore(ListableObjectStore<T> objectStore) {
		this.localObjectStore = objectStore;

		logger.info("Initializing CustomMonitoredObjectStore with objectstore " + objectStore);
	}

	public boolean contains(Serializable key) throws ObjectStoreException {
		logger.debug("contains - key=" + key);
		return this.localObjectStore.contains(key);
	}

	@SuppressWarnings("unchecked")
	public void store(Serializable key, T value) throws ObjectStoreException {
		CustomCacheTSEntry thisCacheEntry = new CustomCacheTSEntry((MuleEvent) value, System.currentTimeMillis());

		logger.info(
				"store - key=" + key + "; value=" + value + ", MuleContext=" + ((MuleEvent) value).getMuleContext());

		this.localObjectStore.store(key, thisCacheEntry);
	}

	public T retrieve(Serializable key) throws ObjectStoreException {
		logger.info("retrieve - key=" + key);
		long initialTime = System.currentTimeMillis();

		CustomCacheTSEntry retrievedObject = (CustomCacheTSEntry) this.localObjectStore.retrieve(key);
		
		logger.info("[CACHE] Retrieve Object from cache (ms):" + (System.currentTimeMillis() - initialTime));
		if (retrievedObject == null) {
			throw new ObjectDoesNotExistException();
		}

		logger.debug("Raw value retrieved from cache: " + retrievedObject);
		logger.info("[CACHE] Create CustomCacheTSEntry (ms):" + (System.currentTimeMillis() - initialTime));
		logger.info("Constructed cache entry =" + key + ",value=NOT SHOWING PAYLOAD, TimeStamp="
				+ retrievedObject.getCreationTimeStamp());
		
		T cachedObject = (T) getEventFromPayload(retrievedObject.getThisMuleEvent(), RequestContext.getEvent());

		logger.info("[CACHE] getEventFromPayload (ms):" + (System.currentTimeMillis() - initialTime));
		return cachedObject;
	}

	public MuleEvent getEventFromPayload(String payload, MuleEvent originalEvent)
	{
		logger.info("getEventFromPayload (" + payload + "), originalEvent=" + originalEvent);
		MuleEvent  thisMuleEvent = null;

		try {

			DefaultMuleMessage messageWithContext = null;
			messageWithContext = new DefaultMuleMessage(payload, this.getMuleContext());

			MuleEvent muleEvent = new DefaultMuleEvent(messageWithContext, originalEvent);
			muleEvent.setMessage(messageWithContext);
			thisMuleEvent = muleEvent;

		} catch (Exception e) {
			logger.error("Thrown error:" + e.getMessage());
			e.printStackTrace();
		}

		logger.info("Created event=" + originalEvent);

		return thisMuleEvent;

	}

	public T remove(Serializable key) throws ObjectStoreException {
		logger.info("remove - key=" + key);
		CustomCacheTSEntry retrievedObject = (CustomCacheTSEntry) this.localObjectStore.remove(key);
		
		logger.info("Removing existing object...");
		/*if (retrievedObject == null) {
			throw new ObjectDoesNotExistException();
		}*/

		return (T) this.getEventFromPayload(retrievedObject.getThisMuleEvent(), RequestContext.getEvent());
	}

	public boolean isPersistent() {
		logger.info("isPersistent : " + this.localObjectStore.isPersistent());
		return this.localObjectStore.isPersistent();
	}

	public void clear() throws ObjectStoreException {
		logger.info("Clearing objectstore - " + this.localObjectStore);
		this.localObjectStore.clear();
	}

	protected void expire() {

		logger.info("expire - entryTTL=" + this.entryTTL + ", expirationInterval=" + this.expirationInterval + ", maxEntries=" + maxEntries);
		int expiredEntries = 0;
		try {
			int currentSize = 0;
			List<Serializable> keys = this.localObjectStore.allKeys();
			logger.info("Keys:" + keys);
			if (keys != null) {
				currentSize = keys.size();
			}

			if ((this.entryTTL > 0) && (currentSize != 0)) {
				long now = System.nanoTime();
				for (Serializable key : keys) {
					CustomCacheTSEntry thisCacheEntry = (CustomCacheTSEntry) this.localObjectStore.retrieve(key);

					String value = thisCacheEntry.getThisMuleEvent();
					long TS = thisCacheEntry.getCreationTimeStamp();

					logger.info("Retrieved entry - key=" + key + "; TimeStamp Written=" + TS);

					long elapsedTime = System.currentTimeMillis() - TS;

					if (elapsedTime > this.entryTTL) {
						logger.info("Removing expired entry from objectStore.  Key=" + key + ", exceeded time="
								+ elapsedTime);
						remove(key);
						expiredEntries++;
					}

				}
			}

			if (logger.isDebugEnabled())
				logger.debug("Expired " + expiredEntries + " old entries");
		} catch (Exception e) {
			logger.error("Error occured during CustomlocalObjectStore.expire()" + e.getCause());
			e.printStackTrace();
		}
	}

	public ListableObjectStore getLocalObjectStore() {
		return localObjectStore;
	}

	public void setLocalObjectStore(ListableObjectStore localObjectStore) {

		logger.info("Setting localObjectStore to => " + localObjectStore);

		this.localObjectStore = localObjectStore;
	}
}