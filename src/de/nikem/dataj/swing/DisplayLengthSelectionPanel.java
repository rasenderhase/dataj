package de.nikem.dataj.swing;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

public class DisplayLengthSelectionPanel extends JPanel {
	private static final long serialVersionUID = -5263307207816223917L;

	private final ServerTableRowSorter rowSorter;
	
	private JComboBox<Integer> displayLengthCombobox;
	
	public DisplayLengthSelectionPanel(ServerTableRowSorter rowSorter) {
		super(new FlowLayout());
		this.rowSorter = rowSorter;
		init();
	}

	private void init() {
		add(getDisplayLengthCombobox());
	}
	
	public JComboBox<Integer> getDisplayLengthCombobox() {
		if (displayLengthCombobox == null) {
			displayLengthCombobox = new JComboBox<Integer>(new Integer[] { 10, 25, 50, 100 });
			Integer actual = rowSorter.getiDisplayLength();
			displayLengthCombobox.setSelectedItem(actual);
			
			resetComponentsStates();
			
			displayLengthCombobox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Integer actual = (Integer) displayLengthCombobox.getSelectedItem();
					rowSorter.setiDisplayLength(actual);
					resetComponentsStates();
				}
			});
		}
		return displayLengthCombobox;
	}

	/**
	 * @param actual
	 */
	protected void resetComponentsStates() {
		String text = DatajResourceBundle.getBundle(getLocale()).getString("sLengthMenu");
		displayLengthCombobox.setToolTipText(text.replaceAll("_MENU_", Integer.toString((Integer) displayLengthCombobox.getSelectedItem())));
	}
}
