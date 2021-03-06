/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx;

import argo.jdom.JdomParser;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import tgfx.system.Machine;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author ril3y
 */
public class TinygDriver extends Observable implements Observer {

//    private MachineTinyG mTG = MachineTinyG.getInstance();
    public Machine m = new Machine();
    private JdomParser JDOM = new JdomParser(); //JSON Object Parser
    /**
     * Static commands for TinyG to get settings from the TinyG Driver Board
     */
    public static final String CMD_GET_OK_PROMPT = "{\"gc\":\"?\"}\n";
    public static final String CMD_GET_STATUS_REPORT = "{\"sr\":\"\"}\n";
    public static final String CMD_DISABLE_LOCAL_ECHO = "{\"ee\":0}\n";
    public static final String CMD_SET_STATUS_UPDATE_INTERVAL = "{\"si\":50}\n";
    private static final String CMD_GET_MACHINE_SETTINGS = "{\"sys\":null}\n";
    private static final String CMD_GET_X_AXIS = "{\"x\":null}\n";
    private static final String CMD_GET_Y_AXIS = "{\"y\":null}\n";
    private static final String CMD_GET_Z_AXIS = "{\"z\":null}\n";
    private static final String CMD_GET_A_AXIS = "{\"a\":null}\n";
    private static final String CMD_GET_B_AXIS = "{\"b\":null}\n";
    private static final String CMD_GET_C_AXIS = "{\"c\":null}\n";
    private static final String CMD_GET_MOTOR_1_SETTINGS = "{\"1\":null}\n";
    private static final String CMD_GET_MOTOR_2_SETTINGS = "{\"2\":null}\n";
    private static final String CMD_GET_MOTOR_3_SETTINGS = "{\"3\":null}\n";
    private static final String CMD_GET_MOTOR_4_SETTINGS = "{\"4\":null}\n";
    private static final String STATUS_REPORT = "\"sr\":";
    private SerialDriver ser = SerialDriver.getInstance();
    private String buf; //Buffer to store parital json lines

    /**
     * Singleton Code for the Serial Port Object
     *
     * @return
     */
    public static TinygDriver getInstance() {
        return TinygDriverHolder.INSTANCE;
    }

    private static class TinygDriverHolder {

        private static final TinygDriver INSTANCE = new TinygDriver();
    }
    //End Singleton

    @Override
    public void update(Observable o, Object o1) {
        String[] MSG = (String[]) o1;
        if (MSG[0] == "JSON") {
            parseJSON(MSG[1]);
        }
    }

    public boolean isPAUSED() {
        return ser.isPAUSED();
    }

    public void setPAUSED(boolean p) throws Exception {
        ser.setPAUSED(p);
        if (p) { //if set to pause
            ser.priorityWrite("!\n");
        } else{ //set to resume
            ser.priorityWrite("~\n");
        }
    }

    public boolean isConnected() {
        return ser.isConnected();
    }

    public void write(String msg) throws Exception {
        ser.write(msg);
    }

    public boolean getClearToSend() {
        return ser.getClearToSend();
    }

    public void priorityWrite(String msg) throws Exception {
        ser.priorityWrite(msg);
    }

    public String[] listSerialPorts() {
        //Get a listing current system serial ports
        String portArray[] = null;
        portArray = ser.listSerialPorts();
        return portArray;
    }

    public void initialize(String portName, int dataRate) {
        this.ser.initialize(portName, dataRate);
        ser.addObserver(this);
//        try {
//            ser.serialPort.addEventListener(this);
//        } catch (TooManyListenersException ex) {
//            System.out.println("ERROR: " + ex.getMessage());
//        } catch (Exception ex) {
//            System.out.println("ERROR: Adding Event Listener");
//        }

    }

//    
    public void disconnect() {
        this.ser.disconnect();
    }

    public String getPortName() {
        //Return the serial port name that is connected.
        return ser.serialPort.getName();
    }

    @Override
    public synchronized void addObserver(Observer obsrvr) {
        super.addObserver(obsrvr);
    }

    @Override
    public void notifyObservers() {
        super.notifyObservers();
    }

    public void getAxisSettings() throws Exception {
        this.ser.write(CMD_GET_A_AXIS);
        System.out.println("Getting XAXIS");
//        return json_response;
    }

    public void parseJSON(String line) {
        try {
            //Create our JSON Parsing Object
            JsonRootNode json = JDOM.parse(line);

            if (line.contains(STATUS_REPORT)) {
                //Parse Status Report
                //"{"sr":{"line":0,"xpos":1.567,"ypos":0.548,"zpos":0.031,"apos":0.000,"vel":792.463,"unit":"mm","stat":"run"}}"
                m.getAxisByName("X").setWork_position(Float.parseFloat(json.getNode("sr").getNode("posx").getText()));
                m.getAxisByName("Y").setWork_position(Float.parseFloat(json.getNode("sr").getNode("posy").getText()));
                m.getAxisByName("Z").setWork_position(Float.parseFloat(json.getNode("sr").getNode("posz").getText()));
                m.getAxisByName("A").setWork_position(Float.parseFloat(json.getNode("sr").getNode("posa").getText()));

                //Parse state out of status report.
                m.setMachineState(Integer.valueOf(json.getNode("sr").getNode("stat").getText()));

                //Parse motion mode (momo) out of start report
                m.setMotionMode(Integer.parseInt(json.getNode("sr").getNode("momo").getText()));

                //Parse velocity out of status report
                m.setVelocity(Float.parseFloat(json.getNode("sr").getNode("vel").getText()));
                
                //Parse Unit Mode
                m.setUnits(Integer.parseInt(json.getNode("sr").getNode("unit").getText()));

                //m.getAxisByName("X").setWork_position(Float.parseFloat((json.getNode("sr").getNode("xpos").getText())));
                //m.getAxisByName("Y").setWork_position(Float.parseFloat((json.getNode("sr").getNode("ypos").getText())));
                //m.getAxisByName("Z").setWork_position(Float.parseFloat((json.getNode("sr").getNode("zpos").getText())));
                //this.A_AXIS.setWork_position(Float.parseFloat((json.getNode("sr").getNode("awp").getText())));
                setChanged();
                notifyObservers("STATUS_REPORT");

            } else if (line.startsWith("{\"sys\":")) {
                System.out.println("[#]Parsing Machine Settings JSON");
                //{"fv":0.930,"fb":330.190,"si":30,"gi":"21","gs":"17","gp":"64","ga":"90","ea":1,"ja":200000.000,"ml":0.080,"ma":0.100,"mt":10000.000,"ic":0,"il":0,"ec":0,"ee":0,"ex":1}
                m.setFirmware_version(Float.parseFloat((json.getNode("fv").getText())));
                m.setFirmware_build(Float.parseFloat((json.getNode("fb").getText())));
                m.setStatus_report_interval(Integer.parseInt((json.getNode("si").getText())));
//                m.setEnable_acceleration(Boolean.parseBoolean((json.getNode("ea").getText())));
//                m.setCorner_acceleration(Integer.parseInt((json.getNode("ja").getText())));
                m.setMin_line_segment(Float.parseFloat((json.getNode("ml").getText())));
                m.setMin_segment_time(Integer.parseInt((json.getNode("mt").getText())));
                m.setMin_arc_segment(Float.parseFloat((json.getNode("ma").getText())));
                m.setIgnore_CR(Boolean.parseBoolean((json.getNode("ic").getText())));
                m.setIgnore_LF(Boolean.parseBoolean((json.getNode("il").getText())));
                m.setEnable_CR(Boolean.parseBoolean((json.getNode("ec").getText())));
                m.setEnable_echo(Boolean.parseBoolean((json.getNode("ee").getText())));
                m.setEnable_xon_xoff(Boolean.parseBoolean((json.getNode("ex").getText())));


            } else if (line.startsWith("{\"1\":") && !line.contains("null")) {
                int motor = 1;
                getSystemSettings(line, motor);
            } else if (line.startsWith("{\"2\":") && !line.contains("null")) {
                int motor = 2;
                getSystemSettings(line, motor);

            } else if (line.startsWith("{\"3\":") && !line.contains("null")) {
                int motor = 3;
                getSystemSettings(line, motor);

            } else if (line.startsWith("{\"4\":") && !line.contains("null")) {
                int motor = 4;
                getSystemSettings(line, motor);

            } else if (line.startsWith("{\"5\":") && !line.contains("null")) {
                int motor = 5;
                getSystemSettings(line, motor);

            } else if (line.startsWith("{\"6\":") && !line.contains("null")) {
                int motor = 6;
                getSystemSettings(line, motor);
            }


        } catch (argo.saj.InvalidSyntaxException ex) {
            System.out.println("[!]ParseJson Exception: " + ex.getMessage() + " LINE: " + line);
            setChanged();
            notifyObservers("[!] " + ex.getMessage()+"Line Was: " + line +"\n");
        } catch (argo.jdom.JsonNodeDoesNotMatchPathElementsException ex) {
            //Extra } for some reason
            System.out.println("[!]ParseJson Exception: " + ex.getMessage() + " LINE: " + line);
            setChanged();
            notifyObservers("[!] " + ex.getMessage()+"Line Was: " + line +"\n");
            
        } catch (Exception ex) {
            setChanged();
            notifyObservers("ERROR");
            System.out.println("Exception in TinygDriver");
            System.out.println(ex.getMessage());
        }
    }

    private synchronized void getSystemSettings(String line, int motor) throws InvalidSyntaxException {
        //{"1":{"ma":0,"sa":1.800,"tr":1.250,"mi":8,"po":0,"pm":1}}
        JsonRootNode json = JDOM.parse(line);
        String strMotor = String.valueOf(motor);


        m.getMotorByNumber(motor).setMapToAxis(Integer.valueOf((json.getNode(strMotor).getNode("ma").getText())));
        m.getMotorByNumber(motor).setStep_angle(Float.valueOf(json.getNode(strMotor).getNode("sa").getText()));
        m.getMotorByNumber(motor).setTravel_per_revolution(Float.valueOf(json.getNode(strMotor).getNode("tr").getText()));
        m.getMotorByNumber(motor).setPolarity(Boolean.valueOf((json.getNode(strMotor).getNode("mi").getText())));
        m.getMotorByNumber(motor).setPower_management(Boolean.valueOf(json.getNode(strMotor).getNode("po").getText()));
        m.getMotorByNumber(motor).setMicrosteps(Integer.valueOf(json.getNode(strMotor).getNode("pm").getText()));


        setChanged();
        notifyObservers("CMD_GET_MACHINE_SETTINGS");
    }

    public void requestStatusUpdate() {
        try {
            ser.write(CMD_GET_STATUS_REPORT);
            setChanged();
            notifyObservers("HEARTBEAT");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void getMachineSettings() {
        try {
            ser.write(CMD_GET_MACHINE_SETTINGS);
            setChanged();
            notifyObservers("CMD_GET_MACHINE_SETTINGS");
        } catch (Exception e) {
            System.out.println("ERROR Writing to Serial Port in getMachineSettings");
        }
    }

    public void getMotorSettings(int motorNumber) {
        try {
            if (motorNumber == 1) {
                ser.write(CMD_GET_MOTOR_1_SETTINGS);
            } else if (motorNumber == 2) {
                ser.write(CMD_GET_MOTOR_2_SETTINGS);
            } else if (motorNumber == 3) {
                ser.write(CMD_GET_MOTOR_3_SETTINGS);
            } else if (motorNumber == 4) {
                ser.write(CMD_GET_MOTOR_4_SETTINGS);
            } else {
                System.out.println("Invalid Motor Number.. Please try again..");
                setChanged();
//                notifyObservers("{\"error\":\"Invalid Motor Number\"}");
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public synchronized boolean hasChanged() {
        return super.hasChanged();
    }

    @Override
    protected synchronized void setChanged() {
        super.setChanged();
    }
}
