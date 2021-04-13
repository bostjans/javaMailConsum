package com.stupica.prog;


import com.stupica.ConstGlobal;
import com.stupica.GlobalVar;

import com.stupica.core.UtilString;
import com.stupica.mainRunner.MainRunBase;
import jargs.gnu.CmdLineParser;

import java.util.Map;
import java.util.logging.Logger;


/**
 * Created by bostjans on 07/03/2021.
 */
public class MainRun extends MainRunBase {
    // Variables
    //
    private int iQueueWaitTime = 1000 * 50;
    private String sQueueAddr = "tcp://localhost:61616";
    private String sQueueName = "vs.mail2send.queue";

    private String sMailAddr = "localhost";
    private String sMailUser = "user";
    private String sMailPsw = "password";

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

    private JmsClient objJmsClient = null;

    private static Logger logger = Logger.getLogger(MainRun.class.getName());


    /**
     * @param a_args    ..
     */
    public static void main(String[] a_args) {
        // Local variables
        int             iReturn;

        // Initialization
        iReturn = ConstGlobal.PROCESS_EXIT_SUCCESS;
        GlobalVar.getInstance().sProgName = "mailConsumer";
        GlobalVar.getInstance().sVersionBuild = "011";

        // Generate main program class
        objInstance = new MainRun();

        iReturn = objInstance.invokeApp(a_args);

        // Return
        if (iReturn != ConstGlobal.PROCESS_EXIT_SUCCESS)
            System.exit(iReturn);
        else
            //System.exit(ConstGlobal.EXIT_SUCCESS);
            return;
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
        bIsRunInLoop = true;
//        GlobalVar.bIsModeDev = true;    // Should be disabled/commented for final artifact build!
//        if (GlobalVar.bIsModeDev) {
//            sQueueAddr = "tcp://artemisdev:61616";
//            //sQueueName = "all.mail2send.queue";
//            iQueueWaitTime = 1000 * 2;
//        }
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
        String sTemp = null;

        sQueueAddr = objPropSett.getProperty("MQ_SOURCE_URL", sQueueAddr);
        sQueueName = objPropSett.getProperty("MQ_QUEUE_NAME", sQueueName);

        sTemp = objPropSett.getProperty("EC.MAX_LOOP");
        if (!UtilString.isEmptyTrim(sTemp)) {
            iTemp = Long.parseLong(sTemp);
        }
        if (iTemp != null)
            iMaxNumOfLoops = iTemp;

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

            Integer iTempInt = (Integer)obj_parser.getOptionValue(obj_op_loopPause, -1);
            if (iTempInt >= 0) {
                iPauseBetweenLoop = iTempInt.intValue() * 1000;
            }
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
        iPauseBetweenLoop = 11;

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
            objJmsClient = new JmsClient();
            objJmsClient.sQueueName = sQueueName;
            iResult = objJmsClient.initialize(sQueueAddr, objJmsClient.sQueueName, objJmsClient.iTypeConsumer, GlobalVar.getInstance().sProgName);
            // Error
            if (iResult != ConstGlobal.RETURN_OK) {
                logger.severe("runBefore(): Error at JMS initialize() operation!");
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
        String      sTemp = null;
        Map         objMsgData = null;

        // Initialization
        iResult = super.runLoopCycle(aobjRefCountData);

        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            objMsgData = objJmsClient.receiveEMail(1000 * 50);
            if (GlobalVar.bIsModeVerbose) {
                logger.info("runLoopCycle(): Data:\n\t" + sTemp);
            }
            if (objMsgData == null) {
                iResult = ConstGlobal.RETURN_ENDOFDATA;
                logger.severe("runLoopCycle(): Error at receiveEMail() operation!"
                        + " Data: NoData!");
            }
        }

        // Check previous step
        if (iResult == ConstGlobal.RETURN_OK) {
            //iResult = processData(sTemp);
            // Error ..
            if (iResult != ConstGlobal.RETURN_OK) {
                logger.severe("runLoopCycle(): Error at processData() operation!"
                        + " Result: " + iResult
                        + "; Data: " + sTemp);
            }
            aobjRefCountData.iCountData++;
        }

        if (       (iResult == ConstGlobal.RETURN_ENDOFDATA)
                || (iResult == ConstGlobal.RETURN_NODATA) || (iResult == ConstGlobal.RETURN_INVALID)) {
            iResult = ConstGlobal.RETURN_OK;
        }
        return iResult;
    }
}
