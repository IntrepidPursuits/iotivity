package org.iotivity.ca.service;

/**
 *
 * Created by Anton Spaans on 5/29/15.
 */
public interface RMResponseReceivedIfc {
    void OnResponseReceived(String subject, String receivedData);
}
