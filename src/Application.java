
public class Application implements Runnable {
	
    public static void main(String[] args) {
    	try
    	{
    		javax.swing.SwingUtilities.invokeLater(new Application());
    	}
    	catch(Exception e)
    	{
    		System.out.println(e);
    	}
    }

	public void run()
	{
		Patcher patcher = new Patcher();
		Gui gui = new Gui(patcher);
		gui.createAndShow();
	}
    
}
