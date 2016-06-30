package com.wpmac.addisplay.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by StarGazer on 2015/11/21.
 */
public class USBDriver {

    final int MAX_NUMBER_BYTES = 65536;
    private int DEFAULT_TIMEOUT = 500;

    private byte[] readBuffer;
    private byte[] usbData;
    private int writeIndex;
    private int readIndex;
    private int readCount;
    private int totalBytes;
    private ArrayList<String> deviceNum = new ArrayList<>();
    private int deviceCount = 0;
    public int writeTimeOutMillis;
    public int readTimeOutMillis;
    private boolean broadcastFlag = false;
    String mString;
    private int bulkPacketSize;

    public boolean READ_ENABLE = false;

    protected final Object readQueueLock = new Object();
    protected final Object writeQueueLock = new Object();

    public ReadThread readThread;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    public UsbInterface usbInterface;
    private Context context;
    private UsbDeviceConnection usbDeviceConnection;
    private PendingIntent pendingIntent;
    private UsbEndpoint bulkInPoint;
    private UsbEndpoint bulkOutPoint;

    public USBDriver(Context context, UsbManager usbManager) {
        readBuffer = new byte[MAX_NUMBER_BYTES];
        usbData = new byte[1024];

        this.usbManager = usbManager;
        this.context = context;
        this.mString = "USB_service";

        writeTimeOutMillis = 10000;
        readTimeOutMillis = 10000;

        addDevice("1a86:7523");
    }

    private void addDevice(String str) {
        deviceNum.add(str);
        deviceCount = deviceNum.size();
    }


    public synchronized void openUsbDevice(UsbDevice usbDevice) {

        Object localObject;
        UsbInterface intf;
        if (usbDevice == null)
            return;
        intf = getUsbInterface(usbDevice);
        if (intf != null) {
            localObject = this.usbManager.openDevice(usbDevice);
            if (localObject != null) {
                if (((UsbDeviceConnection) localObject).claimInterface(intf, true)) {
                    this.usbDevice = usbDevice;
                    this.usbDeviceConnection = ((UsbDeviceConnection) localObject);
                    this.usbInterface = intf;
                    if (!enumerateEndPoint(intf))
                        return;
                    Toast.makeText(context, "Device Has Attached to Android", Toast.LENGTH_LONG).show();
                    if (!READ_ENABLE) {
                        READ_ENABLE = true;
                        readThread = new ReadThread(bulkInPoint, usbDeviceConnection);
                        readThread.start();
                    }
                }
            }
        }
    }

    /**
     * 用于打开usb设备 android.hardware.usb.UsbDevice
     *
     * @param usbDevice
     */
    public synchronized void openDevice(UsbDevice usbDevice) {
        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(mString), 0);
        if (usbManager.hasPermission(usbDevice)) {
            openUsbDevice(usbDevice);
        } else {
            synchronized (usbReceiver) {
                usbManager.requestPermission(usbDevice, pendingIntent);
            }
        }
    }

    public synchronized void closeDevice() {
        try {
            Thread.sleep(10);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (this.usbDeviceConnection != null) {
            if (this.usbInterface != null) {
                this.usbDeviceConnection.releaseInterface(this.usbInterface);
                this.usbInterface = null;
            }

            this.usbDeviceConnection.close();
        }

        if (this.usbDevice != null) {
            this.usbDevice = null;
        }

        if (this.usbManager != null) {
            this.usbManager = null;
        }


        if (READ_ENABLE) {
            READ_ENABLE = false;
        }

		/*
         * No need unregisterReceiver
		 */
        if (broadcastFlag) {
            this.context.unregisterReceiver(usbReceiver);
            broadcastFlag = false;
        }

//		System.exit(0);
    }


    public UsbDevice enumerateDevice() {

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(mString), 0);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            Toast.makeText(context, "No Device Or Device Not Match", Toast.LENGTH_LONG).show();
            return null;
        }
        for (UsbDevice localUsbDevice : deviceList.values()) {
            for (int i = 0; i < deviceCount; ++i) {
//				 Log.d(TAG, "DeviceCount is " + DeviceCount);
                if (String.format("%04x:%04x", new Object[]{localUsbDevice.getVendorId(),
                        localUsbDevice.getProductId()}).equals(deviceNum.get(i))) {
                    IntentFilter filter = new IntentFilter(mString);
                    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                    context.registerReceiver(usbReceiver, filter);
                    broadcastFlag = true;
                    return localUsbDevice;

                } else {
                    Log.d("", "String.format not match");
                }
            }
        }

        return null;
    }

    public boolean isConnected() {
        return (this.usbDevice != null) && (this.usbInterface != null) && (this.usbDeviceConnection != null);
    }

    //过滤器
    public UsbInterface getUsbInterface(UsbDevice usbDevice) {

        if (this.usbDeviceConnection != null) {
            if (this.usbInterface != null) {
                this.usbDeviceConnection.releaseInterface(this.usbInterface);
                this.usbInterface = null;
            }
            this.usbDeviceConnection.close();
            this.usbDevice = null;
            this.usbInterface = null;
        }

        if (usbDevice == null) {
            return null;
        }

        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {

            UsbInterface intf = usbDevice.getInterface(i);
            if (intf.getInterfaceClass() == 0xff
                    && intf.getInterfaceSubclass() == 0x01
                    && intf.getInterfaceProtocol() == 0x02) {
                return intf;
            }
        }
        return null;
    }

    public boolean enumerateEndPoint(UsbInterface sInterface) {
        if (sInterface == null)
            return false;
        for (int i = 0; i < sInterface.getEndpointCount(); ++i) {
            UsbEndpoint endPoint = sInterface.getEndpoint(i);
            if (endPoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && endPoint.getMaxPacketSize() == 0x20) {
                if (endPoint.getDirection() == UsbConstants.USB_DIR_IN) {
                    bulkInPoint = endPoint;
                } else {
                    bulkOutPoint = endPoint;
                }
                this.bulkPacketSize = endPoint.getMaxPacketSize();
            }
        }
        return true;

    }

    /**
     * ********USB broadcast receiver******************************************
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
                return;

            if (mString.equals(action)) {
                synchronized (this) {
                    UsbDevice localUsbDevice = intent.getParcelableExtra("device");
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openUsbDevice(localUsbDevice);
                    } else {
                        Toast.makeText(USBDriver.this.context, "Deny USB Permission", Toast.LENGTH_SHORT).show();
                        Log.d("", "permission denied");
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(USBDriver.this.context, "Disconnect", Toast.LENGTH_SHORT).show();
                //CloseDevice();
            } else {
                Log.d("", "......");
            }
        }
    };


    private class ReadThread extends Thread {
        UsbEndpoint usbEndpoint;
        UsbDeviceConnection usbDeviceConnection;

        public ReadThread(UsbEndpoint usbEndpoint, UsbDeviceConnection usbDeviceConnection) {
            this.usbEndpoint = usbEndpoint;
            this.usbDeviceConnection = usbDeviceConnection;
        }

        @Override
        public void run() {
            while (READ_ENABLE) {
                while (totalBytes > MAX_NUMBER_BYTES - 63) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                synchronized (readQueueLock) {
                    readCount = usbDeviceConnection.bulkTransfer(usbEndpoint, usbData, 64, readTimeOutMillis);
                    if (readCount > 0) {
                        for (int count = 0; count < readCount; count++) {
                            readBuffer[writeIndex] = usbData[count];
                            writeIndex++;
                            writeIndex %= MAX_NUMBER_BYTES;
                        }

                        if (writeIndex >= readIndex)
                            totalBytes = writeIndex - readIndex;
                        else
                            totalBytes = (MAX_NUMBER_BYTES - readIndex) + writeIndex;

                    }
                }
            }
        }
    }

    public int readData(byte[] data, int length) {
//        Log.i("wwwwwwwww", Arrays.toString(readBuffer));
//        Log.i("wwwwwwwww", length + " : " + totalBytes);
        int mLen;

		/*should be at least one byte to read*/
        if ((length < 1) || (totalBytes == 0)) {
            mLen = 0;
            return mLen;
        }

		/*check for max limit*/
        if (length > totalBytes)
            length = totalBytes;

		/*update the number of bytes available*/
        totalBytes -= length;

        mLen = length;

		/*copy to the user buffer*/
        for (int count = 0; count < length; count++) {
            data[count] = readBuffer[readIndex];

            readIndex++;
			/*shouldnt read more than what is there in the buffer,
			 * 	so no need to check the overflow
			 */
            readIndex %= MAX_NUMBER_BYTES;
        }
        Log.i("wwwwwwwww", Arrays.toString(data));
        return mLen;
    }


    //写数据输出部分
    public int writeData(byte[] buf, int length) throws IOException {
        int mLen;
        mLen = writeData(buf, length, this.writeTimeOutMillis);
        if (mLen < 0) {
            throw new IOException("Expected Write Actual Bytes");
        }
        return mLen;
    }

    public int writeData(byte[] buf, int length, int timeoutMillis) {
        int offset = 0;
        int HasWritten;
        int odd_len = length;
        if (this.bulkOutPoint == null)
            return -1;
        while (offset < length) {
            synchronized (this.writeQueueLock) {
                int mLen = Math.min(odd_len, this.bulkPacketSize);
                byte[] arrayOfByte = new byte[mLen];
                if (offset == 0) {
                    System.arraycopy(buf, 0, arrayOfByte, 0, mLen);
                } else {
                    System.arraycopy(buf, offset, arrayOfByte, 0, mLen);
                }
                HasWritten = this.usbDeviceConnection.bulkTransfer(this.bulkOutPoint, arrayOfByte, mLen, timeoutMillis);
                if (HasWritten < 0) {
                    return -2;
                } else {
                    offset += HasWritten;
                    odd_len -= HasWritten;
//					Log.d(TAG, "offset " + offset + " odd_len " + odd_len);
                }
            }
        }
        return offset;
    }


    public boolean SetConfig(int baudRate, byte dataBit, byte stopBit, byte parity, byte flowControl) {
        int value = 0;
        int index = 0;
        char valueHigh = 0, valueLow = 0, indexHigh = 0, indexLow = 0;
        switch (parity) {
            case 0:	/*NONE*/
                valueHigh = 0x00;
                break;
            case 1:	/*ODD*/
                valueHigh |= 0x08;
                break;
            case 2:	/*Even*/
                valueHigh |= 0x18;
                break;
            case 3:	/*Mark*/
                valueHigh |= 0x28;
                break;
            case 4:	/*Space*/
                valueHigh |= 0x38;
                break;
            default:	/*None*/
                valueHigh = 0x00;
                break;
        }

        if (stopBit == 2) {
            valueHigh |= 0x04;
        }

        switch (dataBit) {
            case 5:
                valueHigh |= 0x00;
                break;
            case 6:
                valueHigh |= 0x01;
                break;
            case 7:
                valueHigh |= 0x02;
                break;
            case 8:
                valueHigh |= 0x03;
                break;
            default:
                valueHigh |= 0x03;
                break;
        }

        valueHigh |= 0xc0;
        valueLow = 0x9c;

        value |= valueLow;
        value |= (int) (valueHigh << 8);

        switch (baudRate) {
            case 50:
                indexLow = 0;
                indexHigh = 0x16;
                break;
            case 75:
                indexLow = 0;
                indexHigh = 0x64;
                break;
            case 110:
                indexLow = 0;
                indexHigh = 0x96;
                break;
            case 135:
                indexLow = 0;
                indexHigh = 0xa9;
                break;
            case 150:
                indexLow = 0;
                indexHigh = 0xb2;
                break;
            case 300:
                indexLow = 0;
                indexHigh = 0xd9;
                break;
            case 600:
                indexLow = 1;
                indexHigh = 0x64;
                break;
            case 1200:
                indexLow = 1;
                indexHigh = 0xb2;
                break;
            case 1800:
                indexLow = 1;
                indexHigh = 0xcc;
                break;
            case 2400:
                indexLow = 1;
                indexHigh = 0xd9;
                break;
            case 4800:
                indexLow = 2;
                indexHigh = 0x64;
                break;
            case 9600:
                indexLow = 2;
                indexHigh = 0xb2;
                break;
            case 19200:
                indexLow = 2;
                indexHigh = 0xd9;
                break;
            case 38400:
                indexLow = 3;
                indexHigh = 0x64;
                break;
            case 57600:
                indexLow = 3;
                indexHigh = 0x98;
                break;
            case 115200:
                indexLow = 3;
                indexHigh = 0xcc;
                break;
            case 230400:
                indexLow = 3;
                indexHigh = 0xe6;
                break;
            case 460800:
                indexLow = 3;
                indexHigh = 0xf3;
                break;
            case 500000:
                indexLow = 3;
                indexHigh = 0xf4;
                break;
            case 921600:
                indexLow = 7;
                indexHigh = 0xf3;
                break;
            case 1000000:
                indexLow = 3;
                indexHigh = 0xfa;
                break;
            case 2000000:
                indexLow = 3;
                indexHigh = 0xfd;
                break;
            case 3000000:
                indexLow = 3;
                indexHigh = 0xfe;
                break;
            default:    // default baudRate "9600"
                indexLow = 2;
                indexHigh = 0xb2;
                break;
        }

        index |= 0x88 | indexLow;
        index |= (int) (indexHigh << 8);

        Uart_Control_Out(UartCmd.VENDOR_SERIAL_INIT, value, index);
        if (flowControl == 1) {
            Uart_Tiocmset(UartModem.TIOCM_DTR | UartModem.TIOCM_RTS, 0x00);
        }
        return true;
    }

    public boolean UartInit() {
        int ret;
        int size = 8;
        byte[] buffer = new byte[size];
        Uart_Control_Out(UartCmd.VENDOR_SERIAL_INIT, 0x0000, 0x0000);
        ret = Uart_Control_In(UartCmd.VENDOR_VERSION, 0x0000, 0x0000, buffer, 2);
        if (ret < 0)
            return false;
        Uart_Control_Out(UartCmd.VENDOR_WRITE, 0x1312, 0xD982);
        Uart_Control_Out(UartCmd.VENDOR_WRITE, 0x0f2c, 0x0004);
        ret = Uart_Control_In(UartCmd.VENDOR_READ, 0x2518, 0x0000, buffer, 2);
        if (ret < 0)
            return false;
        Uart_Control_Out(UartCmd.VENDOR_WRITE, 0x2727, 0x0000);
        Uart_Control_Out(UartCmd.VENDOR_MODEM_OUT, 0x00ff, 0x0000);
        return true;
    }

    public int Uart_Control_In(int request, int value, int index, byte[] buffer, int length) {
        int retval = 0;
        retval = usbDeviceConnection.controlTransfer(UsbType.USB_TYPE_VENDOR | UsbType.USB_RECIP_DEVICE | UsbType.USB_DIR_IN,
                request, value, index, buffer, length, DEFAULT_TIMEOUT);
        return retval;
    }

    public int Uart_Control_Out(int request, int value, int index) {
        int retval = 0;
        retval = usbDeviceConnection.controlTransfer(UsbType.USB_TYPE_VENDOR | UsbType.USB_RECIP_DEVICE | UsbType.USB_DIR_OUT,
                request, value, index, null, 0, DEFAULT_TIMEOUT);

        return retval;
    }

    public int Uart_Tiocmset(int set, int clear) {
        int control = 0;
        if ((set & UartModem.TIOCM_RTS) == UartModem.TIOCM_RTS)
            control |= UartIoBits.UART_BIT_RTS;
        if ((set & UartModem.TIOCM_DTR) == UartModem.TIOCM_DTR)
            control |= UartIoBits.UART_BIT_DTR;
        if ((clear & UartModem.TIOCM_RTS) == UartModem.TIOCM_RTS)
            control &= ~UartIoBits.UART_BIT_RTS;
        if ((clear & UartModem.TIOCM_DTR) == UartModem.TIOCM_DTR)
            control &= ~UartIoBits.UART_BIT_DTR;

        return Uart_Set_Handshake(control);
    }

    private int Uart_Set_Handshake(int control) {
        return Uart_Control_Out(UartCmd.VENDOR_MODEM_OUT, ~control, 0);
    }

    public final class UartModem {
        public static final int TIOCM_LE = 0x001;
        public static final int TIOCM_DTR = 0x002;
        public static final int TIOCM_RTS = 0x004;
        public static final int TIOCM_ST = 0x008;
        public static final int TIOCM_SR = 0x010;
        public static final int TIOCM_CTS = 0x020;
        public static final int TIOCM_CAR = 0x040;
        public static final int TIOCM_RNG = 0x080;
        public static final int TIOCM_DSR = 0x100;
        public static final int TIOCM_CD = TIOCM_CAR;
        public static final int TIOCM_RI = TIOCM_RNG;
        public static final int TIOCM_OUT1 = 0x2000;
        public static final int TIOCM_OUT2 = 0x4000;
        public static final int TIOCM_LOOP = 0x8000;
    }

    public final class UsbType {
        public static final int USB_TYPE_VENDOR = (0x02 << 5);
        public static final int USB_RECIP_DEVICE = 0x00;
        public static final int USB_DIR_OUT = 0x00;	/*to device*/
        public static final int USB_DIR_IN = 0x80;  /*to host*/
    }

    public final class UartCmd {
        public static final int VENDOR_WRITE_TYPE = 0x40;
        public static final int VENDOR_READ_TYPE = 0xC0;
        public static final int VENDOR_READ = 0x95;
        public static final int VENDOR_WRITE = 0x9A;
        public static final int VENDOR_SERIAL_INIT = 0xA1;
        public static final int VENDOR_MODEM_OUT = 0xA4;
        public static final int VENDOR_VERSION = 0x5F;
    }

    public final class UartState {
        public static final int UART_STATE = 0x00;
        public static final int UART_OVERRUN_ERROR = 0x01;
        public static final int UART_PARITY_ERROR = 0x02;
        public static final int UART_FRAME_ERROR = 0x06;
        public static final int UART_RECV_ERROR = 0x02;
        public static final int UART_STATE_TRANSIENT_MASK = 0x07;
    }

    public final class UartIoBits {
        public static final int UART_BIT_RTS = (1 << 6);
        public static final int UART_BIT_DTR = (1 << 5);
    }
}