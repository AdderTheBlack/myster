/* 

	Title:			Myster Open Source
	Author:			Andrew Trumper
	Description:	Generic Myster Code
	
	This code is under GPL

Copyright Andrew Trumper 2000-2001
*/

package com.myster.menubar.event;

import Myster;
import java.awt.*;
import java.awt.event.*;

public class QuitMenuAction implements ActionListener {

	
	public void actionPerformed(ActionEvent e) {
		Myster.quit();//System.exit(0);
	}

}