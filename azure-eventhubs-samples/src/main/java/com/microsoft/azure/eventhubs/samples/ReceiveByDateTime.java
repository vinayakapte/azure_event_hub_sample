/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.eventhubs.samples;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.*;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.function.*;
import java.util.logging.*;

import com.microsoft.azure.eventhubs.*;
import com.microsoft.azure.servicebus.*;

public class ReceiveByDateTime
{

	public static void main(String[] args) 
			throws ServiceBusException, ExecutionException, InterruptedException, IOException
	{
		final String namespaceName = "iothub-ns-tsp-device-173741-ce950a26f2";
		final String eventHubName = "tsp-device-hub";
		final String sasKeyName = "iothubowner";
		final String sasKey = "C+IipH2ZdaX6MgibSJNCc6LUt9ZyQHgFDanQWT64Vio=";
		ConnectionStringBuilder connStr = new ConnectionStringBuilder(namespaceName, eventHubName, sasKeyName, sasKey);
		
		EventHubClient ehClient = EventHubClient.createFromConnectionString(connStr.toString()).get();
		String partitionIds[] = null;

		try
		{
			EventHubRuntimeInformation ehInfo = ehClient.getRuntimeInformation().get();
			if (ehInfo != null)
			{
				partitionIds = ehInfo.getPartitionIds();

				System.out.println("Eventhub count of partitions: " + ehInfo.getPartitionCount());
				for (String id : partitionIds)
				{
					System.out.println("Found partition with id: " + id);
				}
			}
			else
			{
				System.out.println("getRuntimeInformation returned null");
			}
		}
		catch (InterruptedException | ExecutionException e)
		{
			System.out.println("Exception: " + e.toString());
		}




		// receiver
		String partitionId = "0"; // API to get PartitionIds will be released as part of V0.2
		
		PartitionReceiver receiver = ehClient.createEpochReceiver(
				EventHubClient.DEFAULT_CONSUMER_GROUP_NAME, 
				partitionId, 
				Instant.now(),
				2345).get();
		
		System.out.println("date-time receiver created...");
		
		try
		{
			while (true)
			{
				receiver.receive(100).thenAccept(new Consumer<Iterable<EventData>>()
				{
					public void accept(Iterable<EventData> receivedEvents)
					{
						int batchSize = 0;
						if (receivedEvents != null)
						{
							for(EventData receivedEvent: receivedEvents)
							{
								System.out.print(String.format("Offset: %s, SeqNo: %s, EnqueueTime: %s", 
										receivedEvent.getSystemProperties().getOffset(), 
										receivedEvent.getSystemProperties().getSequenceNumber(), 
										receivedEvent.getSystemProperties().getEnqueuedTime()));
								System.out.println(String.format("| Message Payload: %s", new String(receivedEvent.getBody(), Charset.defaultCharset())));
								batchSize++;
							}
						}
						
						System.out.println(String.format("ReceivedBatch Size: %s", batchSize));
					}
				}).get();
			}
		}
		finally
		{
			// this is paramount; max number of concurrent receiver per consumergroup per partition is 5
			receiver.close().get();
		}
	}

	/**
	 * actual application-payload, ex: a telemetry event
	 */
	static final class PayloadEvent
	{
		PayloadEvent()	{}
		
		public String strProperty;
		public long longProperty;
		public int intProperty;
	}

}
