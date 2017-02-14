import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Gui
{
	JFrame mFrame;
	JPanel mPanel;
	JPanel mOptionPanel;
	JLabel mFileLabel;
	JLabel mOSLabel;
	JButton mBrowseButton;
	JButton mPatchButton;
	String mFilePath = new String(".");
	String[] mTimeouts;
	Integer[] mRetry = {5,6,7,8,9,10,11,12,13};
	JComboBox<String> mTimeoutComboBox;
	JComboBox<Integer> mRetryComboBox;
	JCheckBox mDisableNewPollCheckbox;
	JCheckBox mEnableColdstartCheckbox;
	JCheckBox mResetAtarimaxCheckbox;
	
	Patcher mPatcher;
	
	Gui(Patcher patcher)
	{
		mTimeouts = new String[15];
		for(int i=0;i<15;i++)
		{
			mTimeouts[i] = new String("NTSC:"+(i+1)*16+"ms / PAL:"+(i+1)*20+"ms");
		}
		mPatcher = patcher;
	}
	
	
	void createAndShow()
	{
		mFrame = new JFrame("SIO2BT OS Patcher V3.1 Montezuma 2014-2017");
		mPanel = new JPanel();
		mFileLabel = new JLabel("File:                                                                                          ");
		mOSLabel = new JLabel("OS:");
		
		mBrowseButton = new JButton("Browse");
		mBrowseButton.addActionListener(
				new ActionListener() {
		    		public void actionPerformed(ActionEvent e)
		    		{
		    			JFileChooser fc = new JFileChooser(mFilePath);
		    			fc.setDialogType(JFileChooser.OPEN_DIALOG);
		    			if(JFileChooser.APPROVE_OPTION == fc.showOpenDialog(mFrame))
		    			{
		    				File f = fc.getSelectedFile();
		    				mFilePath = f.getPath();
		    				
		    				setDefaults();
		    				
		    				if(mPatcher.setOS(f))
		    				{
		    					mFileLabel.setText("File: " + f.getName());
		    					mOSLabel.setText("OS: " + mPatcher.getOSName());
		    					
		    					mPatchButton.setEnabled(true);
		    					mTimeoutComboBox.setEnabled(true);
		    					mRetryComboBox.setEnabled(true);
		    					
		    					boolean newPoll = mPatcher.isNewPollPatchable();
		    					mDisableNewPollCheckbox.setEnabled(newPoll);
		    					mDisableNewPollCheckbox.setSelected(newPoll);
		    					mPatcher.setDisableNewPoll(newPoll);
		    					
		    					boolean coldStart = mPatcher.isColdstartPatchable();
		    					mEnableColdstartCheckbox.setEnabled(coldStart);
		    					mEnableColdstartCheckbox.setSelected(coldStart);
		    					mPatcher.setEnableColdStart(coldStart);
		    					
		    					boolean atarimaxReset = mPatcher.isAtarimaxResetable();
		    					mResetAtarimaxCheckbox.setEnabled(coldStart && atarimaxReset);
		    					mResetAtarimaxCheckbox.setSelected(coldStart && atarimaxReset);
		    					mPatcher.setResetAtarimax(coldStart && atarimaxReset);
		    				}
		    				else
		    				{
		    					mFileLabel.setText("File:");
		    					mOSLabel.setText("OS:");
		    					JOptionPane.showMessageDialog(mFrame, f.getName()+" is not supported", null, JOptionPane.ERROR_MESSAGE);
		    				}
		    			}
		    		}
				}
				);
		
		mPatchButton = new JButton("Patch");
		mPatchButton.addActionListener(
				new ActionListener() {
		    		public void actionPerformed(ActionEvent e)
		    		{
		    			if(mPatcher.patch())
		    			{
		    				JOptionPane.showMessageDialog(mFrame, "Patching successful", null, JOptionPane.INFORMATION_MESSAGE);
		    			}
		    			else
		    			{
		    				JOptionPane.showMessageDialog(mFrame, "Patching failed", null, JOptionPane.ERROR_MESSAGE);
		    			}
		    			
		    			disableGUIControls();
		    		}
				}
				);
		
		mPanel.setLayout(new BoxLayout(mPanel, BoxLayout.X_AXIS));
		mPanel.add(mFileLabel);
		mPanel.add(Box.createHorizontalGlue());
		mPanel.add(mBrowseButton);
		mPanel.add(new JLabel("    "));
		
		mOptionPanel = new JPanel();
		mOptionPanel.setLayout(new BoxLayout(mOptionPanel, BoxLayout.Y_AXIS));
		
		mTimeoutComboBox = new JComboBox<String>(mTimeouts);
		mTimeoutComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = mTimeoutComboBox.getSelectedIndex();
				mPatcher.setTimeout(index+2);
			}
		});
		mOptionPanel.add(new JLabel("TIMEOUT:"));
		mOptionPanel.add(mTimeoutComboBox);
		
		mRetryComboBox = new JComboBox<Integer>(mRetry);
		mRetryComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mPatcher.setRetry(mRetryComboBox.getSelectedIndex()+5);
			}
		});
		mOptionPanel.add(new JLabel("RETRY COUNT:"));
		mOptionPanel.add(mRetryComboBox);
		
		mDisableNewPollCheckbox = new JCheckBox("Disable poll for new devices");
		mDisableNewPollCheckbox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				mPatcher.setDisableNewPoll((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		mOptionPanel.add(mDisableNewPollCheckbox);

		mEnableColdstartCheckbox = new JCheckBox("Enable cold start with Shift+Reset");
		mEnableColdstartCheckbox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED)
				{
					mPatcher.setEnableColdStart(true);
					mPatcher.setResetAtarimax(mPatcher.isAtarimaxResetable());
					mResetAtarimaxCheckbox.setSelected(mPatcher.isAtarimaxResetable());
					mResetAtarimaxCheckbox.setEnabled(mPatcher.isAtarimaxResetable());
				}
				else
				{
					mPatcher.setEnableColdStart(false);
					mPatcher.setResetAtarimax(false);
					mResetAtarimaxCheckbox.setSelected(false);
					mResetAtarimaxCheckbox.setEnabled(false);
				}
			}
		});
		mOptionPanel.add(mEnableColdstartCheckbox);		
		
		mResetAtarimaxCheckbox = new JCheckBox("Select Atarimax Bank 0 at Coldstart");
		mResetAtarimaxCheckbox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				mPatcher.setResetAtarimax((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		mOptionPanel.add(mResetAtarimaxCheckbox);		
	
		mFrame.getContentPane().add(mOSLabel, BorderLayout.NORTH);
		mFrame.getContentPane().add(mPanel, BorderLayout.CENTER);
		mFrame.getContentPane().add(mOptionPanel, BorderLayout.EAST);
		mFrame.getContentPane().add(mPatchButton, BorderLayout.SOUTH);
		
		setDefaults();

		mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mFrame.pack();
		mFrame.setResizable(false);
		mFrame.setVisible(true);
	}
	
	
	void setDefaults()
	{
		mTimeoutComboBox.setSelectedIndex(14);
		mRetryComboBox.setSelectedIndex(0);
		mDisableNewPollCheckbox.setSelected(false);
		mEnableColdstartCheckbox.setSelected(false);
		mResetAtarimaxCheckbox.setSelected(false);
		mPatcher.setDisableNewPoll(false);
		mPatcher.setEnableColdStart(false);
		mPatcher.setResetAtarimax(false);
		disableGUIControls();
	}
	
	void disableGUIControls()
	{
		mPatchButton.setEnabled(false);
		mTimeoutComboBox.setEnabled(false);
		mRetryComboBox.setEnabled(false);
		mDisableNewPollCheckbox.setEnabled(false);
		mEnableColdstartCheckbox.setEnabled(false);
		mResetAtarimaxCheckbox.setEnabled(false);
	}
	
}
