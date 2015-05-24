// AlivePacket.java
// Alive Technologies
//
// Parses data from Bluetooth SPP connected Alive Heart Monitor

package com.example.nikhil.industrial_project.packets;


public class AliveHeartMonitorPacket {
    private static final int BUFFER_SIZE = 512;

    public AliveHeartMonitorPacket() { init();  }
    public int getPacketLength() { return mPacketLength; }
    public byte[] getPacketData() { return mBuffer; }
    public int getSeqNum() { return mSeqNum; }
    public int getPrevSeqNum() { return mPrevSeqNum; }
    public byte getInfo() { return mInfo; }
    public int getECGLength() { return mECGLength; }
    public int getECGDataIndex() { return mECGDataIndex; }
    public byte getECGID() { return mECGID; }
    public byte getECGDataFormat() { return mECGFormat; }
    public int getECGSamplingRate() { return (300); }
    public int getAccLength() { return mAccLength; }
    public int getAccDataIndex() { return mAccDataIndex; }
    public byte getAccID() { return mAccID; }
    public byte getAccDataFormat() { return mAccFormat; }
    public int getAccChannels() { return mAccChannels; }
    public int getAccSamplingRate() { return (75); }
    public double getBatteryPercent() { return (mBattPercent); } // if less than 0, then unknown.
    

    private final static int ESTATE_SYNC = 0;
    private final static int ESTATE_PACKETHEADER = 5;
    private final static int ESTATE_DATAHEADER = 6;
    private final static int ESTATE_DATA = 7;
    private final static int ESTATE_CHECKSUM = 8;
    private int mChannelByteCount;
    private byte mChannelType;
    private byte mChannelFormat;
    private int mDataChannel;
    private int mDataChannels;
    private int mChannelPacketLength;
    private int mPacketLength;
    private int mPacketBytes;
    private int mECGLength;
    private byte mECGID;
    private byte mECGFormat;
    private int mECGDataIndex;
    private int mAccChannels;
    private int mAccLength;
    private byte mAccFormat;
    private byte mAccID;
    private int mAccDataIndex;
    private byte[] mBuffer = new byte[BUFFER_SIZE];
    private byte mPacketCheckSum;
    private byte mInfo;
    private int mSeqNum;
    private int mPrevSeqNum;
    private double mBattPercent;
    private int mState;

    public void init() {
        mECGLength = 0;
        mPacketLength = 0;
        mPacketBytes = 0;
        mSeqNum = -1;
        mPrevSeqNum = -1;
        mInfo = 0;
        mECGFormat = 0;
        mAccLength = 0;
        mAccChannels = 0;
        mAccFormat = 0;
        mBattPercent = -1.0; // -1=Unknown, 0-100%
        mState = ESTATE_SYNC;
    }

    public boolean add(byte ucData) {
        mBuffer[mPacketBytes] = ucData;
        mPacketBytes++;
        switch (mState) {
            case ESTATE_SYNC:
                if (mPacketBytes == 1) {
                    if (ucData == (byte) 0x00) {
                        // Start of new packet
                        mPacketCheckSum = 0; // Start new checksum
                        mECGLength = 0;
                        mAccLength = 0;
                        mPrevSeqNum = mSeqNum;
                    } else {
                        mPacketBytes = 0;
                    }
                } else if (mPacketBytes == 2) {
                    if (ucData == (byte) 0xFE) {
                        mState = ESTATE_PACKETHEADER;
                    } else {
                        mState = ESTATE_SYNC;
                        mPacketBytes = 0;
                    }
                }
                break;

            case ESTATE_PACKETHEADER:
                if (mPacketBytes == 3) {
                    mBattPercent = ucData / 2.0;
                } else if (mPacketBytes == 4) {
                    mInfo = (byte) ((ucData & (byte) 0xF0) >> 4);	// Top nibble for info
                    mSeqNum = (ucData & 0x0F) << 8;	// Bottom nibble is the high 4 bits of 12 bit sequence number
                } else if (mPacketBytes == 5) {
                    mSeqNum |= (ucData & 0xFF);		// Low 8 bits of 12 bit sequence number
                } else if (mPacketBytes == 6) {
                    mDataChannels = ucData;
                    mDataChannel = 0;
                    mChannelByteCount = 0;

                    // Packet length:
                    //	6  byte main header
                    //  n1 bytes in channel 1
                    //  n2 bytes in channel 2
                    //  ...
                    //  1  byte checksum + bytes in each channel

                    // Set current packet length and checksum.
                    // We add length of channel bytes after reading each channel header
                    mPacketLength = mPacketBytes + 1;
                    mState = ESTATE_DATAHEADER;
                }
                break;
            case ESTATE_DATAHEADER:
                mChannelByteCount++;
                if (mChannelByteCount == 1) {
                    mChannelType = ucData;
                } else if (mChannelByteCount == 2) {
                    mChannelPacketLength = (ucData & 0xFF) << 8;
                } else if (mChannelByteCount == 3) {
                    mChannelPacketLength |= (ucData & 0xFF);

                    // mChannelPacketLength includes header and data bytes
                    // add this to the packet length
                    mPacketLength += mChannelPacketLength;
                    if (mPacketLength >= BUFFER_SIZE) {
                        mState = ESTATE_SYNC;
                        mPacketBytes = 0;
                        System.err.println("Packet is too large");
                    }
                } else if (mChannelByteCount == 4) {
                    mChannelFormat = ucData;

                    // Note: any additional header bytes are ignored

                    mState = ESTATE_DATA;
                }
                break;
            case ESTATE_DATA:
                mChannelByteCount++;
                if (mChannelByteCount == mChannelPacketLength) {
                    if (mChannelType == (byte) 0xAA) {
                        // ECG data packet
                        mECGID = mChannelType;
                        mECGFormat = mChannelFormat;
                        mECGLength = mChannelPacketLength - 5; 		// 5 byte ECG header
                        mECGDataIndex = mPacketBytes - mECGLength;


                    } else if (mChannelType == (byte) 0x56) {
                        // Acc 3 channel data packet
                        mAccID = mChannelType;

                        mAccChannels = 3;
                        mAccFormat = mChannelFormat;
                        mAccLength = mChannelPacketLength - 5; // 5 byte Acc header
                        mAccDataIndex = mPacketBytes - mAccLength;
                    } else if (mChannelType == (byte) 0x55) {
                        // Obsolete. Ignore Acc 2 channel data packet
                    } else {
                        // Unknown channel type. Just ignore
                    }
                    mDataChannel++;
                    if (mDataChannel == mDataChannels) {
                        mState = ESTATE_CHECKSUM;
                    } else {
                        mChannelByteCount = 0;
                        mState = ESTATE_DATAHEADER;
                    }
                }
                break;
            case ESTATE_CHECKSUM:
                if (ucData == mPacketCheckSum) {
                    mState = ESTATE_SYNC;
                    mPacketBytes = 0;
                    return (true);
                }
                else {
                    System.err.println("Bad checksum");
                    mState = ESTATE_SYNC;
                    mPacketBytes = 0;
                }
                break;

        }
        mPacketCheckSum += ucData;
        return (false);
    }
}