package com.stupica.prog;


import com.stupica.ConstGlobal;
import com.stupica.GlobalVar;
import com.stupica.queue.JmsClientMail;

import com.stupica.core.UtilString;
import com.stupica.mainRunner.MainRunBase;
import jargs.gnu.CmdLineParser;

import javax.jms.MapMessage;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * Created by bostjans on 07/03/2021.
 */
public class MainRun extends MainRunBase {
    // Variables
    //
    private String sQueueAddr = "tcp://localhost:61616";
    private String sQueueName = "vs.mail2send.queue";

    private String sMailAddr = "localhost";
    private String sMailUser = "user";
    private String sMailPsw = "password";
    private String sFromConfig = null;
    private String sReplyToConfig = null;
    private String sContTypeConfig = null;
    private String sFooterConfig = null;

    /**
     * Main object instance variable;
     */
    private static MainRun objInstance;

    CmdLineParser.Option obj_op_source;
    CmdLineParser.Option obj_op_dest;
    CmdLineParser.Option obj_op_user;
    CmdLineParser.Option obj_op_psw;
    CmdLineParser.Option obj_op_max;
    CmdLineParser.Option obj_op_loopPause;

    private JmsClientMail objJmsClient = null;

    private static Logger logger = Logger.getLogger(MainRun.class.getName());


    /**
     * @param a_args    ..
     */
    public static void main(String[] a_args) {
        // Initialization
        GlobalVar.getInstance().sProgName = "mailConsumer";
        GlobalVar.getInstance().sVersionBuild = "015";

        // Generate main program class
        objInstance = new MainRun();

        iReturnCode = objInstance.invokeApp(a_args);

        // Return
        if (iReturnCode != ConstGlobal.PROCESS_EXIT_SUCCESS)
            System.exit(iReturnCode);
    }


    protected void printUsage() {
        super.printUsage();
        System.err.println("            [{-s,--source} a_source_url]");
        System.err.println("            [{-d,--dest} a_mail_host]");
        System.err.println("            [{-u,--user} a_mail_username]");
        System.err.println("            [{-p,--psw} a_mail_password]");
        System.err.println("            [{-m,--maxLoops} max loops to import");
        System.err.println("            [{--loopPause} a_time_to_wait_between_loop");
    }


    /**
     * Method: initialize
     *
     * ..
     */
    protected void initialize() {
        super.initialize();
        bIsProcessInLoop = true;
    }


    /**
     * Method: setConfig
     *
     * ..
     *
     * @return int 	1 = AllOK;
     */
    public int setConfig() {
        Long iTemp = null;
        String sTemp;

        sQueueAddr = objPropSett.getProperty("MQ_SOURCE_URL", sQueueAddr);
        sQueueName = objPropSett.getProperty("MQ_QUEUE_NAME", sQueueName);

        sTemp = objPropSett.getProperty("EC.MAX_LOOP");
        if (!UtilString.isEmptyTrim(sTemp)) {
            iTemp = Long.parseLong(sTemp);
        }
        if (iTemp != null)
            iMaxNumOfLoops = iTemp;

        sFromConfig = objPropSett.getProperty("EM_FROM", null);
        sReplyToConfig = objPropSett.getProperty("EM_REPLYTO", null);
        sContTypeConfig = objPropSett.getProperty("EM_CONTENT_TYPE", "text/plain");
        sFooterConfig = objPropSett.getProperty("EM_FOOTER", null);

        return ConstGlobal.RETURN_OK;
    }


    /**
     * Method: defineArguments
     *
     * ..
     *
     * @return int iResult	1 = AllOK;
     */
    protected int defineArguments() {
        // Local variables
        int         iResult;

        // Initialization
        iResult = super.defineArguments();

        obj_op_source = obj_parser.addStringOption('s', "source");
        obj_op_dest = obj_parser.addStringOption('d', "dest");
        obj_op_user = obj_parser.addStringOption('u', "user");
        obj_op_psw = obj_parser.addStringOption('p', "psw");
        obj_op_max = obj_parser.addLongOption('m', "maxLoops");
        obj_op_loopPause = obj_parser.addIntegerOption("loopPause");
        return iResult;
    }

    /**
     * Method: readArguments
     *
     * ..
     *
     * @return int iResult	1 = AllOK;
     */
    protected int readArguments() {
        // Local variables
        int         iResult;

        // Initialization
        iResult = super.readArguments();

        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            // Set program parameter
            objInstance.sQueueAddr = (String)obj_parser.getOptionValue(obj_op_source, objInstance.sQueueAddr);
        }
        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            // Set program parameter
            objInstance.sMailAddr = (String)obj_parser.getOptionValue(obj_op_dest, objPropSett.getProperty("EM_HOST", sMailAddr));
            objInstance.sMailUser = (String)obj_parser.getOptionValue(obj_op_user, objPropSett.getProperty("EM_USER", ""));
            objInstance.sMailPsw = (String)obj_parser.getOptionValue(obj_op_psw, objPropSett.getProperty("EM_PSW", ""));
        }
        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            objInstance.iMaxNumOfLoops = (Long)obj_parser.getOptionValue(obj_op_max, iMaxNumOfLoops);

            //Integer iTempInt = (Integer)obj_parser.getOptionValue(obj_op_loopPause, 1);
            //if (iTempInt >= 0) {
            //    iPauseBetweenLoop = iTempInt.intValue() * 1000;
            //}
        }
        return iResult;
    }


    /**
     * Method: runBefore
     *
     * Run ..
     *
     * @return int	1 = AllOK;
     */
    protected int runBefore() {
        // Local variables
        int         iResult;

        // Initialization
        iResult = super.runBefore();

        // Init ..
        iPauseBetweenLoop = 1;

        // Check ..
        //
        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            if (UtilString.isEmpty(sQueueAddr)) {
                iResult = ConstGlobal.RETURN_ERROR;
                logger.severe("runBefore(): Error at input verification - input NOT defined!"
                        + " UrlIN: " + sQueueAddr);
            }
        }

        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            objJmsClient = new JmsClientMail();
            objJmsClient.sQueueName = sQueueName;
            objJmsClient.setSessionMode(javax.jms.Session.CLIENT_ACKNOWLEDGE);
            objJmsClient.bIgnoreException = true;
            iResult = objJmsClient.initialize(sQueueAddr, objJmsClient.sQueueName, objJmsClient.iTypeConsumer,
                    GlobalVar.getInstance().sProgName + iInstanceNum);
            // Error
            if (iResult != ConstGlobal.RETURN_OK) {
                logger.severe("runBefore(): Error at JMS initialize() operation!");
            } else {
                logger.info("runBefore(): JmsClient() connected: " + sQueueAddr);
            }
        }
        return iResult;
    }

    /**
     * Method: runAfter
     *
     * Run ..
     *
     * @return int	1 = AllOK;
     */
    protected int runAfter() {
        // Local variables
        int         iResult;

        // Initialization
        iResult = super.runAfter();
        if (objJmsClient != null) {
            objJmsClient.disconnect();
        }
        return iResult;
    }


    /**
     * Method: runLoopCycle
     *
     * Run_Loop_cycle ..
     *
     * @return int	1 = AllOK;
     */
    protected int runLoopCycle(RefDataInteger aobjRefCountData) {
        // Local variables
        int         iResult;
        //Map         objMsgData = null;
        MapMessage  objMsgData = null;

        // Initialization
        iResult = super.runLoopCycle(aobjRefCountData);

        // Check ..
        if (!objJmsClient.isConnected()) {
            iResult = objJmsClient.reconnect();
            if (iResult != ConstGlobal.RETURN_OK) {
                if (objJmsClient.bIgnoreException) {
                    iResult = ConstGlobal.RETURN_OK;
                    logger.warning("runLoopCycle(): Lost connection to MQ!"
                            + " Ignoring > will continue .. ");
                    iPauseBetweenLoop = 1000 * 50;
                    return iResult;
                } else {
                    logger.severe("runLoopCycle(): Lost connection to MQ!"
                            + " Stopping .. ");
                    return iResult;
                }
            }
            iPauseBetweenLoop = 1;
        }

        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            objMsgData = objJmsClient.receiveEMail(1000 * 50);
            //if (GlobalVar.bIsModeVerbose) {
            //    logger.info("runLoopCycle(): Data:\n\t" + sTemp);
            //}
            if (objMsgData == null) {
                iResult = ConstGlobal.RETURN_ENDOFDATA;
                logger.warning("runLoopCycle(): Error at receiveEMail() operation!"
                        + " Data: NoData!");
            }
        }

        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            iResult = processData(objMsgData);
            // Error ..
            if (iResult != ConstGlobal.RETURN_OK) {
                logger.severe("runLoopCycle(): Error at processData() operation!"
                        + " Result: " + iResult);
            }
            aobjRefCountData.iCountData++;
        }
        // Check previous step
        if ((iResult == ConstGlobal.RETURN_OK) || (iResult == ConstGlobal.RETURN_WARN)) {
            if (objMsgData != null) {
                try {
                    objMsgData.acknowledge();
                    logger.info("runLoopCycle(): .. message acknowledge."
                            + " MsgId: " + objJmsClient.getMsgIdLast()
                            + "; Msg.: /");
                } catch (Exception ex) {
                    if (iResult == ConstGlobal.RETURN_OK)
                        iResult = ConstGlobal.RETURN_ERROR;
                    logger.severe("runLoopCycle(): Error at message acknowledge!"
                            + " URI: " + sQueueAddr
                            + "; MsgId: " + objJmsClient.getMsgIdLast()
                            + "; Msg.: " + ex.getMessage());
                }
            }
        } else {
            if (objJmsClient.isConnected()) {
                iResult = objJmsClient.recover();
                logger.info("runLoopCycle(): .. message recover!"
                        + " MsgId: " + objJmsClient.getMsgIdLast()
                        + "; Msg.: /");
            }
        }

        if (       (iResult == ConstGlobal.RETURN_ENDOFDATA)
                || (iResult == ConstGlobal.RETURN_NODATA) || (iResult == ConstGlobal.RETURN_INVALID)) {
            iResult = ConstGlobal.RETURN_OK;
        }
        return iResult;
    }


    /**
     * Method: processData
     *
     * Run_Loop_cycle ..
     *
     * @return int	1 = AllOK;
     */
    protected int processData(MapMessage aobjData) {
        // Local variables
        int         iResult;
        String      sTemp;
        String      sContentType = null;

        // Initialization
        iResult = ConstGlobal.RETURN_OK;

        //if (aobjData.containsKey(JmsClient.sKeyContentType))
        try {
            if (aobjData.itemExists(JmsClientMail.sKeyContentType))
                sContentType = aobjData.getString(JmsClientMail.sKeyContentType);
            else
                sContentType = sContTypeConfig;
        } catch (Exception ex) {
            iResult = ConstGlobal.RETURN_ERROR;
            logger.severe("processData(): Error at message operation (extraction)!"
                    + " Operation: ?"
                    + "; Msg.: " + ex.getMessage());
        }

        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            // Set the host SMTP address
            Properties props = new Properties();
            props.put("mail.smtp.host", sMailAddr);

            // create some properties and get the default Session
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(false);

            // create a message
            Message msg = new MimeMessage(session);

            try {
                InternetAddress addressFrom;

                // .. set the from and to address
                if (aobjData.itemExists(JmsClientMail.sKeyFromEMail)) {
                    addressFrom = new InternetAddress(aobjData.getString(JmsClientMail.sKeyFromEMail));
                } else
                    addressFrom = new InternetAddress(sFromConfig);
                msg.setFrom(addressFrom);

                InternetAddress[] addressTo = new InternetAddress[1];
                addressTo[0] = new InternetAddress(aobjData.getString(JmsClientMail.sKeyToEMail));
                msg.setRecipients(Message.RecipientType.TO, addressTo);

                if (aobjData.itemExists(JmsClientMail.sKeyCcEMail)) {
                    InternetAddress[] addressCc = new InternetAddress[1];
                    addressCc[0] = new InternetAddress(aobjData.getString(JmsClientMail.sKeyCcEMail));
                    msg.setRecipients(Message.RecipientType.CC, addressCc);
                }

                // Optional : You can also set your custom headers in the Email if you Want
                msg.addHeader("MyHeaderName", "mailConsumer-" + sQueueName);

                // Setting the Subject and Content Type
                msg.setSubject(aobjData.getString(JmsClientMail.sKeySubject));
                sTemp = (String) aobjData.getObject(JmsClientMail.sKeyContent);
                if (!UtilString.isEmptyTrim(sFooterConfig))
                    sTemp += sFooterConfig;
                msg.setContent(sTemp, sContentType);
                Transport.send(msg);
            } catch (Exception ex) {
                iResult = ConstGlobal.RETURN_ERROR;
                logger.severe("evaluateActualBuy(): Error at sending mail!!"
                        + " Msg.: " + ex.getMessage());
            }
        }
        return iResult;
    }
}
