package com.stupica.prog;


import com.stupica.ConstGlobal;
import com.stupica.GlobalVar;
import com.stupica.queue.JmsClientBase;

import javax.jms.MapMessage;
import javax.jms.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class JmsClient extends JmsClientBase {

    public static final String	sKeyFromEMail	= "from";
    public static final String	sKeyFromName	= "fromName";
    public static final String	sKeyToEMail		= "to";
    public static final String	sKeyToName		= "toName";
    public static final String	sKeyCcEMail		= "cc";
    public static final String	sKeyCcName		= "ccName";
    public static final String	sKeySubject		= "subject";
    public static final String	sKeyContent		= "content";

    private static Logger logger = Logger.getLogger(JmsClient.class.getName());


    /**
     * Object constructor
     */
    public JmsClient() {
        super();
        iMsgTTL = 1000 * 60 * 60 * 24 * 61;
        //sQueueAddr = "tcp://localhost:61616";
        sQueueName = "all.mail2send.queue";
        sClientId = "mailConsumer.programId";
    }


    /**
     * Method: receive
     *
     * Read ..
     *
     * @return Message objMessage	notNull = AllOK;
     */
    public Message receive(int aiQueueWaitTime) {
        // Local variables
        int             iResult;
        Message         objMessage = null;

        // Initialization
        iResult = ConstGlobal.RETURN_OK;

        objMessage = super.receive(aiQueueWaitTime);
        if (GlobalVar.bIsModeVerbose) {
            if (objMessage == null) {
                logger.info("receive(): No data received! Continue ..");
            } else {
                logger.info("receive(): Data received. Continue ..");
            }
        }
        if (objMessage != null) {
            if (GlobalVar.bIsModeVerbose) {
                try {
                    System.out.println("= Message Receive - type: " + objMessage.getJMSType());
                } catch (Exception ex) {
                    iResult = ConstGlobal.RETURN_ERROR;
                    logger.severe("receive(): Error at message operation!"
                            + " Operation: getJMSType()"
                            + "; Msg.: " + ex.getMessage());
                }
            }
        }
        return objMessage;
    }

    /**
     * Method: receiveEMail
     *
     * Read ..
     *
     * @return Map sMsg	notNull = AllOK;
     */
    public Map receiveEMail(int aiQueueWaitTime) {
        // Local variables
        int             iResult;
        HashMap         objData = null;
        Message         objMessage;
        MapMessage objQuMsg = null;

        // Initialization
        iResult = ConstGlobal.RETURN_OK;

        objMessage = receive(aiQueueWaitTime);
        if (objMessage != null) {
            if (objMessage instanceof MapMessage) {
                objQuMsg = (MapMessage)objMessage;
                try {
                    objData = new HashMap();
                    objData.put(sKeyFromEMail, objQuMsg.getString(sKeyFromEMail));
                    if (objQuMsg.itemExists(sKeyFromName))
                        objData.put(sKeyFromName, objQuMsg.getString(sKeyFromName));
                    objData.put(sKeyToEMail, objQuMsg.getString(sKeyToEMail));
                    if (objQuMsg.itemExists(sKeyToName))
                        objData.put(sKeyToName, objQuMsg.getString(sKeyToName));

                    if (objQuMsg.itemExists(sKeyCcEMail))
                        objData.put(sKeyCcEMail, objQuMsg.getString(sKeyCcEMail));
                    if (objQuMsg.itemExists(sKeyCcName))
                        objData.put(sKeyCcName, objQuMsg.getString(sKeyCcName));

                    if (objQuMsg.itemExists(sKeySubject))
                        objData.put(sKeySubject, objQuMsg.getString(sKeySubject));
                    if (objQuMsg.itemExists(sKeyContent))
                        objData.put(sKeyContent, objQuMsg.getString(sKeyContent));
                } catch (Exception ex) {
                    iResult = ConstGlobal.RETURN_ERROR;
                    logger.severe("receiveEMail(): Error at message operation (extraction)!"
                            + " Operation: ?"
                            + "; Msg.: " + ex.getMessage());
                }
            } else {
                logger.warning("receiveEMail(): = Message of unknown Type! Ignoring ..");
            }
        }
        return objData;
    }

}
