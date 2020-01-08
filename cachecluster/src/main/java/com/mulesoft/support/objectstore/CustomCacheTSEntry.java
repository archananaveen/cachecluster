package com.mulesoft.support.objectstore;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mule.api.MuleEvent;

public class CustomCacheTSEntry implements Serializable {
	private static final long serialVersionUID = 1L;
	private String thisMuleEvent;
	private long creationTimeStamp;

	private static final Logger logger = Logger.getLogger(CustomCacheTSEntry.class);

	public CustomCacheTSEntry(MuleEvent thisMuleEvent, long creationTimeStamp) {

		super();

		try {

			this.thisMuleEvent = thisMuleEvent.getMessage().getPayloadAsString();
			this.creationTimeStamp = creationTimeStamp;

			logger.info("Created new " + this);

		} catch (Exception e) {
			logger.error("Error creating CustoCacheTSEntry:" + e.getMessage());
			e.printStackTrace();
		}

	}

	public String getThisMuleEvent() {
		return thisMuleEvent;
	}

	public void setThisMuleEvent(String thisMuleEvent) {
		this.thisMuleEvent = thisMuleEvent;
	}

	public long getCreationTimeStamp() {
		return creationTimeStamp;
	}

	public void setCreationTimeStamp(long creationTimeStamp) {
		this.creationTimeStamp = creationTimeStamp;
	}


	/*public String toString() {
		try {

			return "[" + String.valueOf(this.getCreationTimeStamp()) + "]:" + this.getThisMuleEvent();

		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return "";
		}
	}*/

}
