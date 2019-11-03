import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Patcher
{
	enum eOSSize
	{
		e10K,
		e16K
	}
	
	enum eOSType
	{
		e800A_NTSC,
		e800A_PAL,
		e800B_NTSC,
		e800B_PAL,
		eAA0R10, // 1200XL(A)
		eAA1R11, // 1200XL(B)
		eBB0R1,  // 600XL
		eBB1R2,  // 800XL
		eBB1R3,  // XE
		eBB1R4,  // XEGS
		eQMEG,   // QMEG
		eBB1R2HS,// 800XL + HI-SPEED patch
		eBB1R3HS // XE + HI-SPEED patch
	}
	
	// FREE MEMORY
	// AA0R10 E48F-E4C0 (49 bytes)
	// AA1R11 C68E-C69F (17 bytes)!
	// BB0R1  E49E-E4C0 (34 bytes)
	// BB1R2  E49E-E4C0 (34 bytes)
	// BB1R3  E4B2-E4C0 (14 bytes)!
	// BB1R4  E4B2-E4C0 (14 bytes)!
	
	private static byte [] CS_RESET_PROC = {	
			(byte)0xa9, (byte)0x08, 			// LDA #$08
			(byte)0x2c, (byte)0x0f, (byte)0xd2, // BIT SKSTAT  ;SHIFT pressed?
			(byte)0xd0, (byte)0x03, 			// BNE CSEXIT  ;branch if it isn't
			(byte)0x8d, (byte)0x3d, (byte)0x03,	// STA PUPBT1  ;trash the signature
			(byte)0x4c	};						// CSEXIT JMP RES ;continue with the system reset vectors

	private static byte [] CSAM_RESET_PROC = {	
			(byte)0xa9, (byte)0x08, 			// LDA #$08
			(byte)0x2c, (byte)0x0f, (byte)0xd2, // BIT SKSTAT  ;SHIFT pressed?
			(byte)0xd0, (byte)0x08, 			// BNE CSEXIT  ;branch if it isn't
			(byte)0x8d, (byte)0x3d, (byte)0x03,	// STA PUPBT1  ;trash the signature
			(byte)0xa9, (byte)0x00, 			// LDA #$00
			(byte)0x8d, (byte)0x00, (byte)0xd5, // STA $D500   ;select bank 0 for a Atarimax
			(byte)0x4c	};						// CSEXIT JMP RES ;continue with the system reset vectors

	private byte BASIC_OFF = (byte)0xD0;
	
	private static int OLD_CRETRI1 = 0xE975;
	private static int OLD_CRETRI2 = 0xE9CC;
	private static int OLD_A_CTIM = 0xEC9C;
	private static int OLD_B_CTIM = 0xEC98;
	
	private static int OLD_A_NTSC_CH1 = 0x1168;
	private static int OLD_A_NTSC_CH2 = 0xF638;
	private static int OLD_A_NTSC_CH3 = 0x57DD;
	private static String OLD_A_NTSC_NAME = "400/800 Rev.A NTSC (1979-06)";

	private static int OLD_A_PAL_CH1 = 0x1168;
	private static int OLD_A_PAL_CH2 = 0xF6F9;
	private static int OLD_A_PAL_CH3 = 0x57D6;
	private static String OLD_A_PAL_NAME = "400/800 Rev.A PAL (1979-06)";

	private static int OLD_B_NTSC_CH1 = 0x1168;
	private static int OLD_B_NTSC_CH2 = 0x0000;
	private static int OLD_B_NTSC_CH3 = 0xE6F3;
	private static int OLD_B_NTSC_EXPECTED_CH1 = 0x1168;
	private static int OLD_B_NTSC_EXPECTED_CH2 = 0xec9f;
	private static int OLD_B_NTSC_EXPECTED_CH3 = 0x5829;
	private static String OLD_B_NTSC_NAME = "400/800 Rev.B NTSC (1981-09)";

	private static int OLD_B_PAL_CH1 = 0x1168;
	private static int OLD_B_PAL_CH2 = 0xED60;
	private static int OLD_B_PAL_CH3 = 0x5822;
	private static String OLD_B_PAL_NAME = "400/800 Rev.B PAL (1981-09)";
	
	private static int AA0R10_VER = 0;
	private static int AA0R10_REV = 10;
	private static int AA0R10_CH1 = 0x14A9;
	private static int AA0R10_CH2 = 0xE5BF;
	private static int AA0R10_CRETRI1 = 0xE76B;
	private static int AA0R10_CRETRI2 = 0xE7C4;
	private static int AA0R10_CTIM = 0xEADA;
	private static int AA0R10_PHR = 0xC4DC;
	private static int AA0R10_CSRES = 0xE48F;
	private static String AA0R10_NAME = "1200XL(A): AA000000 Rev. 10 (1982-10-26)";

	private static int AA1R11_VER = 1;
	private static int AA1R11_REV = 11;
	private static int AA1R11_CH1 = 0x9462;
	private static int AA1R11_CH2 = 0x7740;
	private static int AA1R11_CRETRI1 = 0xE98E;
	private static int AA1R11_CRETRI2 = 0xE9E7;
	private static int AA1R11_CTIM = 0xECBD;
	private static int AA1R11_PHR = 0xC45B;
	private static int AA1R11_CSRES = 0xC68E;
	private static String AA1R11_NAME = "1200XL(B): AA000001 Rev. 11 (1982-12-23)";

	private static int BB0R1_VER = 0;
	private static int BB0R1_REV = 1;
	private static int BB0R1_CH1 = 0x9BBF;
	private static int BB0R1_CH2 = 0x6B18;
	private static int BB0R1_CRETRI1 = 0xE98E;
	private static int BB0R1_CRETRI2 = 0xE9E7;
	private static int BB0R1_CTIM = 0xECBD;
	private static int BB0R1_PHR = 0xC41E;
	private static int BB0R1_CSRES = 0xE49E;
	private static int BB0R1_BAS_SELECTION = 0xC4AD;
	private static String BB0R1_NAME = "600XL: BB000000 Rev. 1 (1983-03-11)";

	private static int BB1R2_VER = 1;
	private static int BB1R2_REV = 2;
	private static int BB1R2_CH1 = 0x9211;
	private static int BB1R2_CH2 = 0x6C8C;
	private static int BB1R2_CRETRI1 = 0xE98E;
	private static int BB1R2_CRETRI2 = 0xE9E7;
	private static int BB1R2_CTIM = 0xECBD;
	private static int BB1R2_PHR = 0xC410;
	private static int BB1R2_CSRES = 0xE49E;
	private static int BB1R2_BAS_SELECTION = 0xC49F;
	private static String BB1R2_NAME = "800XL: BB000001 Rev. 2 (1983-05-10)";

	private static int BB1R2HS_CH1 = 0x5C5F;
	private static int BB1R2HS_CH2 = 0x6D7A;
	private static int BB1R2HS_CRETRI1 = 0xCCEB;
	private static int BB1R2HS_CRETRI2 = 0xCE13;
	private static int BB1R2HS_CTIM = 0xCE45;
	private static String BB1R2HS_NAME = "800XL: BB000001 Rev. 2 (1983-05-10) HI-SPEED";

	private static int BB1R3HS_CH1 = 0x53C4;
	private static int BB1R3HS_CH2 = 0x7163;
	private static int BB1R3HS_CRETRI1 = 0xCCEB;
	private static int BB1R3HS_CRETRI2 = 0xCE13;
	private static int BB1R3HS_CTIM = 0xCE45;
	private static String BB1R3HS_NAME = "XE: BB000001 Rev. 3 (1985-03-01) HI-SPEED";
	
	private static int BB1R3_VER = 1;
	private static int BB1R3_REV = 3;
	private static int BB1R3_CH1 = 0x8976;
	private static int BB1R3_CH2 = 0x7075;
	private static int BB1R3_CRETRI1 = 0xE98E;
	private static int BB1R3_CRETRI2 = 0xE9E7;
	private static int BB1R3_CTIM = 0xECBD;
	private static int BB1R3_PHR = 0xC410;
	private static int BB1R3_CSRES = 0xE4B2;
	private static int BB1R3_BAS_SELECTION = 0xC49F;
	private static String BB1R3_NAME = "XE: BB000001 Rev. 3 (1985-03-01)";
	
	private static int BB1R4_VER = 1;
	private static int BB1R4_REV = 4;
	private static int BB1R4_CH1 = 0xAA04;
	private static int BB1R4_CH2 = 0x70A7;
	private static int BB1R4_CRETRI1 = 0xE98E;
	private static int BB1R4_CRETRI2 = 0xE9E7;
	private static int BB1R4_CTIM = 0xECBD;
	private static int BB1R4_PHR = 0xC410;
	private static int BB1R4_CSRES = 0xE4B2;
	private static int BB1R4_BAS_SELECTION = 0xC49F;
	private static String BB1R4_NAME = "XEGS: BB000001 Rev. 4 (1987-05-07)";
	
	private static int QMEG_CHECKSUM = 0x9B;
	private static int QMEG_CRETRI1 = 0xE97D;
	private static int QMEG_CRETRI2 = 0xE9E7;
	private static int QMEG_CTIM1 = 0xECBD;
	private static int QMEG_CTIM2 = 0xED62;
	private static String QMEG_NAME = "QMEG+OS 4.04";
	
	private String mFileName;
	private byte[] mOS;
	private byte[] mOSFillment;
	private String mOSName;
	private eOSSize mOSSize;
	private eOSType mOSType;
	private int mTimeout = 0x10;
	private int mRetry = 0x05;
	private boolean mDisableNewPoll = false;
	private boolean mEnableColdStart = false;
	private boolean mResetAtarimax = false;
	private boolean mDisableBasic = false;
	private boolean mPatchHiSpeed = false;
	
	boolean setOS(File file)
	{
		mFileName = file.getPath();
		switch((int)file.length())
		{
			case 0x4000:
				mOS = new byte[0x4000];
				mOSSize = eOSSize.e16K;
				break;
			case 0x2800:
				mOS = new byte[0x2800];
				mOSSize = eOSSize.e10K;
				break;
			default:
				return false;
		}
		
		try
		{
			InputStream in = new BufferedInputStream(new FileInputStream(file));
			in.read(mOS);
			in.close();
		}
		catch(IOException e)
		{
			return false;
		}
		
		switch(mOSSize)
		{
		case e16K:
			return identify16K();
		case e10K:
			return identify10K();
		}
		
		return false;
	}
	
	private boolean identify16K()
	{
		int ver = (mOS[0x3ff6] & 0xFF);
		int rev = (mOS[0x3ff7] & 0xFF);
		int checksum1 = (mOS[0x0000] & 0xFF) + (0x100 * (mOS[0x0001] & 0xFF)); 
		int checksum2 = (mOS[0x3ff8] & 0xFF) + (0x100 * (mOS[0x3ff9] & 0xFF));
		
		mOSFillment = null;
		
		if(AA0R10_VER==ver && AA0R10_REV==rev && AA0R10_CH1==checksum1 && AA0R10_CH2==checksum2 && is16KChecksumOK(checksum1, checksum2))
		{
			mOSType = eOSType.eAA0R10;
			mOSName = AA0R10_NAME;
		}
		else if(AA1R11_VER==ver && AA1R11_REV==rev && AA1R11_CH1==checksum1 && AA1R11_CH2==checksum2 && is16KChecksumOK(checksum1, checksum2))
		{
			mOSType = eOSType.eAA1R11;
			mOSName = AA1R11_NAME;
		}
		else if(BB0R1_VER==ver && BB0R1_REV==rev && BB0R1_CH1==checksum1 && BB0R1_CH2==checksum2 && is16KChecksumOK(checksum1, checksum2))
		{
			mOSType = eOSType.eBB0R1;
			mOSName = BB0R1_NAME;
		}
		else if(BB1R2_VER==ver && BB1R2_REV==rev && BB1R2_CH1==checksum1 && BB1R2_CH2==checksum2 && is16KChecksumOK(checksum1, checksum2))
		{
			mOSType = eOSType.eBB1R2;
			mOSName = BB1R2_NAME;
		}
		else if(BB1R2_VER==ver && BB1R2_REV==rev && BB1R2HS_CH1==checksum1 && BB1R2HS_CH2==checksum2 && is16KChecksumOK(checksum1, checksum2))
		{
			mOSType = eOSType.eBB1R2HS;
			mOSName = BB1R2HS_NAME;
		}
		else if(BB1R3_VER==ver && BB1R3_REV==rev && BB1R3HS_CH1==checksum1 && BB1R3HS_CH2==checksum2 && is16KChecksumOK(checksum1, checksum2))
		{
			mOSType = eOSType.eBB1R3HS;
			mOSName = BB1R3HS_NAME;
		}
		else if(BB1R3_VER==ver && BB1R3_REV==rev && BB1R3_CH1==checksum1 && BB1R3_CH2==checksum2 && is16KChecksumOK(checksum1, checksum2))
		{
			mOSType = eOSType.eBB1R3;
			mOSName = BB1R3_NAME;
		}
		else if(BB1R4_VER==ver && BB1R4_REV==rev && BB1R4_CH1==checksum1 && BB1R4_CH2==checksum2 && is16KChecksumOK(checksum1, checksum2))
		{
			mOSType = eOSType.eBB1R4;
			mOSName = BB1R4_NAME;
		}
		else if((mOS[0]==(byte)QMEG_CHECKSUM) && isQMEGChecksumOK(QMEG_CHECKSUM))
		{
			mOSType = eOSType.eQMEG;
			mOSName = QMEG_NAME;
		}
		else
		{
			mOSFillment = new byte[0x1800];
			System.arraycopy(mOS, 0, mOSFillment, 0, 0x1800);
			byte [] tmp = new byte[0x2800];
			System.arraycopy(mOS, 0x1800, tmp, 0, tmp.length);
			mOS = tmp;
			return identify10K();
		}
		return true;
	}

	
	private boolean identify10K()
	{
		int checksum1 = (mOS[0x07fe] & 0xFF) + (0x100 * (mOS[0x07ff] & 0xFF)); 
		int checksum2 = (mOS[0x0c0f] & 0xFF) + (0x100 * (mOS[0x0c1f] & 0xFF));
		int checksum3 = (mOS[0x27f8] & 0xFF) + (0x100 * (mOS[0x27f9] & 0xFF));
		if(OLD_A_NTSC_CH1==checksum1 && OLD_A_NTSC_CH2==checksum2 && OLD_A_NTSC_CH3==checksum3 && is10KChecksumOK(checksum1, checksum2, checksum3))
		{
			mOSType = eOSType.e800A_NTSC;
			mOSName = OLD_A_NTSC_NAME;					
		}
		else if(OLD_A_PAL_CH1==checksum1 && OLD_A_PAL_CH2==checksum2 && OLD_A_PAL_CH3==checksum3 && is10KChecksumOK(checksum1, checksum2, checksum3))
		{
			mOSType = eOSType.e800A_PAL;
			mOSName = OLD_A_PAL_NAME;
		}
		else if(OLD_B_NTSC_CH1==checksum1 && OLD_B_NTSC_CH2==checksum2 && OLD_B_NTSC_CH3==checksum3 && 
				is10KChecksumOK(OLD_B_NTSC_EXPECTED_CH1,OLD_B_NTSC_EXPECTED_CH2,OLD_B_NTSC_EXPECTED_CH3) )
		{
			mOSType = eOSType.e800B_NTSC;
			mOSName = OLD_B_NTSC_NAME;
		}
		else if(OLD_B_PAL_CH1==checksum1 && OLD_B_PAL_CH2==checksum2 && OLD_B_PAL_CH3==checksum3 && is10KChecksumOK(checksum1, checksum2, checksum3))
		{
			mOSType = eOSType.e800B_PAL;
			mOSName = OLD_B_PAL_NAME;
		}
		else
		{
			return false;
		}
		return true;
	}
	
	String getOSName()
	{
		return mOSName;
	}
	
	void setTimeout(int timeout)
	{
		mTimeout = timeout;
	}
	
	void setRetry(int retry)
	{
		mRetry = retry;
	}

	void setDisableNewPoll(boolean disableNewPoll)
	{
		mDisableNewPoll = disableNewPoll;
	}
	
	void setEnableColdStart(boolean enableColdStart)
	{
		mEnableColdStart = enableColdStart;
	}

	void setResetAtarimax(boolean reset)
	{
		mResetAtarimax = reset;
	}

	void setDisableBasic(boolean disable_basic)
	{
		mDisableBasic = disable_basic;
	}
	
	void setEnableHiSpeedPatch(boolean enable_hispeed)
	{
		mPatchHiSpeed = enable_hispeed; 
	}
	
	boolean isNewPollPatchable()
	{
		switch(mOSType)
		{
		case e800A_PAL:
		case e800B_PAL:
		case e800A_NTSC:
		case e800B_NTSC:
		case eQMEG:
			return false;
		default:
			return true;
		}
	}

	boolean isColdstartPatchable()
	{
		switch(mOSType)
		{
		case eAA0R10:
		case eAA1R11:
		case eBB0R1:
		case eBB1R2:
		case eBB1R3:
		case eBB1R4:
		case eBB1R2HS:
		case eBB1R3HS:
			return true;
		default:
			return false;
		}
	}

	boolean isAtarimaxResetable()
	{
		switch(mOSType)
		{
		case eAA0R10:
		case eBB0R1:
		case eBB1R2:
		case eBB1R2HS:
			return true;
		default:
			return false;
		}
	}

	boolean isBasicInvertable()
	{
		switch(mOSType)
		{
		case e800A_PAL:
		case e800B_PAL:
		case e800A_NTSC:
		case e800B_NTSC:
		case eAA0R10:
		case eAA1R11:
		case eQMEG:
			return false;
		default:
			return true;
		}
	}
	
	boolean isBasicOff()
	{
		switch(mOSType)
		{
		case e800A_PAL:
		case e800B_PAL:
		case e800A_NTSC:
		case e800B_NTSC:
		case eAA0R10:
		case eAA1R11:
		case eQMEG:
			return true;
		default:
			return false;
		}
	}
	
	boolean isHiSpeedPatchable()
	{
		switch(mOSType)
		{
		case eBB1R2:
		case eBB1R3:
			return true;
		default:
			return false;
		}
		
	}
	
	boolean patch()
	{
		switch(mOSType)
		{
		case e800A_NTSC:
			patch10KOS(OLD_CRETRI1, OLD_CRETRI2, OLD_A_CTIM);
			break;
		case e800A_PAL:
			patch10KOS(OLD_CRETRI1, OLD_CRETRI2, OLD_A_CTIM);
			break;
		case e800B_NTSC:
			patch10KOS(OLD_CRETRI1, OLD_CRETRI2, OLD_B_CTIM);
			break;
		case e800B_PAL:
			patch10KOS(OLD_CRETRI1, OLD_CRETRI2, OLD_B_CTIM);
			break;
		case eAA0R10:
			patch16KOS(AA0R10_CRETRI1, AA0R10_CRETRI2, AA0R10_CTIM, AA0R10_PHR, AA0R10_CSRES, 0);
			break;
		case eAA1R11:
			patch16KOS(AA1R11_CRETRI1, AA1R11_CRETRI2, AA1R11_CTIM, AA1R11_PHR, AA1R11_CSRES, 0);
			break;
		case eBB0R1:
			patch16KOS(BB0R1_CRETRI1, BB0R1_CRETRI2, BB0R1_CTIM, BB0R1_PHR, BB0R1_CSRES, BB0R1_BAS_SELECTION);
			break;
		case eBB1R2:
			patch16KOS(BB1R2_CRETRI1, BB1R2_CRETRI2, BB1R2_CTIM, BB1R2_PHR, BB1R2_CSRES, BB1R2_BAS_SELECTION);
			break;
		case eBB1R2HS:
			patch16KHSOS(BB1R2_CRETRI1, BB1R2_CRETRI2, BB1R2_CTIM, BB1R2HS_CRETRI1, BB1R2HS_CRETRI2, BB1R2HS_CTIM, BB1R2_PHR, BB1R2_CSRES, BB1R2_BAS_SELECTION);
			break;
		case eBB1R3:
			patch16KOS(BB1R3_CRETRI1, BB1R3_CRETRI2, BB1R3_CTIM, BB1R3_PHR, BB1R3_CSRES, BB1R3_BAS_SELECTION);
			break;
		case eBB1R3HS:
			patch16KHSOS(BB1R3_CRETRI1, BB1R3_CRETRI2, BB1R3_CTIM, BB1R3HS_CRETRI1, BB1R3HS_CRETRI2, BB1R3HS_CTIM, BB1R3_PHR, BB1R3_CSRES, BB1R3_BAS_SELECTION);
			break;
		case eBB1R4:
			patch16KOS(BB1R4_CRETRI1, BB1R4_CRETRI2, BB1R4_CTIM, BB1R4_PHR, BB1R4_CSRES, BB1R4_BAS_SELECTION);
			break;
		case eQMEG:
			patchQMEG(QMEG_CRETRI1,QMEG_CRETRI2,QMEG_CTIM1,QMEG_CTIM2);
			break;
		}
		
		try
		{
			OutputStream out = new BufferedOutputStream(new FileOutputStream(mFileName+"_PATCHED"));
			if(mOSFillment!=null)
			{
				out.write(mOSFillment);	
			}
			out.write(mOS);
			out.close();
		}
		catch(IOException e)
		{
			return false;
		}
		
		return true;
	}

	private boolean is10KChecksumOK(int checksum1, int checksum2, int checksum3)
	{
		int calculated_checksum1 = calculate10KChecksum1();
		int calculated_checksum2 = calculate10KChecksum2();
		int calculated_checksum3 = calculate10KChecksum3();
		return ((checksum1==calculated_checksum1) && (checksum2==calculated_checksum2) && (checksum3==calculated_checksum3));
	}
	
	private boolean is16KChecksumOK(int checksum1, int checksum2)
	{
		int calculated_checksum1 = calculate16KChecksum1();
		int calculated_checksum2 = calculate16KChecksum2();
		return ((checksum1==calculated_checksum1) && (checksum2==calculated_checksum2));
	}
	
	private boolean isQMEGChecksumOK(int checksum)
	{
		int calculated_checksum = calculateQMEGChecksum();
		return (checksum==calculated_checksum);
	}

	private void patch10KOS(int CRETRI1, int CRETRI2, int CTIM)
	{
		int offset = 0xD800;
		mOS[CRETRI1-offset] = (byte)mRetry;
		mOS[CRETRI2-offset] = (byte)mRetry;
		mOS[CTIM-offset]    = (byte)mTimeout;
		set10KChecksum();
	}
	
	private void patch16KOS(int CRETRI1, int CRETRI2, int CTIM, int PHR, int CSRES, int BAS_SEL)
	{
		int offset = 0xC000;
		mOS[CRETRI1-offset] = (byte)mRetry;
		mOS[CRETRI2-offset] = (byte)mRetry;
		mOS[CTIM-offset]    = (byte)mTimeout;
		
		if(mDisableNewPoll)
		{
			mOS[PHR+0-offset]   = (byte)0xEA;
			mOS[PHR+1-offset]   = (byte)0xEA;
			mOS[PHR+2-offset]   = (byte)0xEA;
		}
		
		if(mEnableColdStart)
		{
			byte res_low  = mOS[0x3ffc];
			byte res_high = mOS[0x3ffd];
			
			if(mResetAtarimax)
			{
				System.arraycopy(CSAM_RESET_PROC, 0, mOS, CSRES-offset, CSAM_RESET_PROC.length);
				mOS[CSRES+CSAM_RESET_PROC.length-offset] = res_low;
				mOS[CSRES+CSAM_RESET_PROC.length+1-offset] = res_high;
			}
			else
			{
				System.arraycopy(CS_RESET_PROC, 0, mOS, CSRES-offset, CS_RESET_PROC.length);
				mOS[CSRES+CS_RESET_PROC.length-offset] = res_low;
				mOS[CSRES+CS_RESET_PROC.length+1-offset] = res_high;
			}
			
			mOS[0x3ffc] = (byte)(CSRES & 0xFF);
			mOS[0x3ffd] = (byte)((CSRES >> 8) & 0xFF);
		}
		
		if(isBasicInvertable())
		{
			if(mDisableBasic)
			{
				mOS[BAS_SEL-offset] = BASIC_OFF;
			}
		}
		
		if(isHiSpeedPatchable())
		{
			if(mPatchHiSpeed)
			{
				// copy highspeed SIO code to ROM OS
				System.arraycopy(HiSpeed.hispeed_code, 0, mOS, HiSpeed.HIBASE-offset, HiSpeed.hispeed_code.length);
				
				// patch timeout and retries in HI SPEED code
				mOS[0xCD29-offset] = (byte)mRetry;
				mOS[0xCE51-offset] = (byte)mRetry;
				mOS[0xCE83-offset] = (byte)mTimeout;
				
				// copy old standard SIO code to highspeed SIO code
				System.arraycopy(mOS, HiSpeed.XL_SIO-offset, mOS, HiSpeed.HISTDSIO-offset, HiSpeed.newcode.length);

				// add "jump to old code + 4"
				System.arraycopy(HiSpeed.xl_oldcode, 0, mOS, HiSpeed.HISTDSIO+HiSpeed.newcode.length-offset, HiSpeed.xl_oldcode.length);

				// change old SIO code
				System.arraycopy(HiSpeed.newcode, 0, mOS, HiSpeed.XL_SIO-offset, HiSpeed.newcode.length);
			}
		}
		
		set16KChecksum();
	}
	
	// XL HI-SPEED
	private void patch16KHSOS(int CRETRI1, int CRETRI2, int CTIM, int CRETRIHS1, int CRETRIHS2, int CTIMHS, int PHR, int CSRES, int BAS_SEL)
	{
		int offset = 0xC000;
		mOS[CRETRI1-offset] = (byte)mRetry;
		mOS[CRETRI2-offset] = (byte)mRetry;
		mOS[CTIM-offset]    = (byte)mTimeout;
		mOS[CRETRIHS1-offset] = (byte)mRetry;
		mOS[CRETRIHS2-offset] = (byte)mRetry;
		mOS[CTIMHS-offset]    = (byte)mTimeout;
		
		if(mDisableNewPoll)
		{
			mOS[PHR+0-offset]   = (byte)0xEA;
			mOS[PHR+1-offset]   = (byte)0xEA;
			mOS[PHR+2-offset]   = (byte)0xEA;
		}

		if(mEnableColdStart)
		{
			byte res_low  = mOS[0x3ffc];
			byte res_high = mOS[0x3ffd];
			
			if(mResetAtarimax)
			{
				System.arraycopy(CSAM_RESET_PROC, 0, mOS, CSRES-offset, CSAM_RESET_PROC.length);
				mOS[CSRES+CSAM_RESET_PROC.length-offset] = res_low;
				mOS[CSRES+CSAM_RESET_PROC.length+1-offset] = res_high;
			}
			else
			{
				System.arraycopy(CS_RESET_PROC, 0, mOS, CSRES-offset, CS_RESET_PROC.length);
				mOS[CSRES+CS_RESET_PROC.length-offset] = res_low;
				mOS[CSRES+CS_RESET_PROC.length+1-offset] = res_high;
			}
			
			mOS[0x3ffc] = (byte)(CSRES & 0xFF);
			mOS[0x3ffd] = (byte)((CSRES >> 8) & 0xFF);
		}

		if(isBasicInvertable())
		{
			if(mDisableBasic)
			{
				mOS[BAS_SEL-offset] = BASIC_OFF;
			}
		}
		
		set16KChecksum();
	}
	
	private void patchQMEG(int CRETRI1, int CRETRI2, int CTIM1, int CTIM2)
	{
		int offset = 0xC000;
		mOS[CRETRI1-offset] = (byte)mRetry;
		mOS[CRETRI2-offset] = (byte)mRetry;
		mOS[CTIM1-offset]    = (byte)mTimeout;
		mOS[CTIM2-offset]    = (byte)mTimeout;
		
		mOS[0x5] = (byte)0x2B; // - <- +
		mOS[0x6] = (byte)0x42; // OS NAME <- BT
		mOS[0x7] = (byte)0x54;
		
		mOS[0xF35] = (byte)0xA2; // OS NAME <- BT
		mOS[0xF36] = (byte)0xB4;
		
		setQMEGChecksum();
	}
	
	private void set10KChecksum()
	{
		int calculated_checksum1 = calculate10KChecksum1();
		int calculated_checksum2 = calculate10KChecksum2();
		int calculated_checksum3 = calculate10KChecksum3();
		mOS[0x07fe] = (byte)(calculated_checksum1 & 0xFF);
		mOS[0x07ff] = (byte)((calculated_checksum1 >> 8) & 0xFF);
		mOS[0x0c0f] = (byte)(calculated_checksum2 & 0xFF);
		mOS[0x0c1f] = (byte)((calculated_checksum2 >> 8) & 0xFF);
		mOS[0x27f8] = (byte)(calculated_checksum3 & 0xFF);
		mOS[0x27f9] = (byte)((calculated_checksum3 >> 8) & 0xFF);
	}
	
	private void set16KChecksum()
	{
		int calculated_checksum1 = calculate16KChecksum1();
		int calculated_checksum2 = calculate16KChecksum2();
		mOS[0x0000] = (byte)(calculated_checksum1 & 0xFF);
		mOS[0x0001] = (byte)((calculated_checksum1 >> 8) & 0xFF);
		mOS[0x3ff8] = (byte)(calculated_checksum2 & 0xFF);
		mOS[0x3ff9] = (byte)((calculated_checksum2 >> 8) & 0xFF);
	}
	
	private void setQMEGChecksum()
	{
		int calculated_qmeg_checksum = calculateQMEGChecksum(); 
		mOS[0x0000] = (byte)(calculated_qmeg_checksum);
	}
	
	private int calculate10KChecksum1()
	{
		/* Checksum of FPP ROM. */
		int sum = 0;
		for (int i = 0x0000; i < 0x07fe; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		return (sum & 0xffff);
	}
	
	private int calculate10KChecksum2()
	{
		/* Checksum of first 4K ROM. */
		int sum = 0;
		for (int i = 0x0800; i < 0x0c0f; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		for (int i = 0x0c10; i < 0x0c1f; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		for (int i = 0x0c20; i < 0x1800; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		return (sum & 0xffff);
	}
	
	private int calculate10KChecksum3()
	{
		/* Checksum of second 4K ROM. */
		int sum = 0;
		for (int i = 0x1800; i < 0x27f8; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		for (int i = 0x27fa; i < 0x2800; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		return (sum & 0xffff);
	}
	
	private int calculate16KChecksum1()
	{
		/* Checksum of first 8K ROM. */
		int sum = 0;
		for (int i = 0x0002; i < 0x2000; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		return (sum & 0xffff);
	}
	
	private int calculate16KChecksum2()
	{
		/* Checksum of second 8K ROM. */
		int sum = 0;
		for (int i = 0x2000; i < 0x3ff8; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		for (int i = 0x3ffa; i < 0x4000; ++i)
		{
			sum += (mOS[i] & 0xFF);
		}
		return (sum & 0xffff);
	}

	private int calculateQMEGChecksum()
	{
		int sum = 0;
		for (int i = 0x0001 ; i < 0x4000; ++i)
		{
			sum += (int)(mOS[i] & 0xFF);
			sum = sum & 0xFF;
		}
		return ((0x100-sum) & 0xFF);
	}
	
}
