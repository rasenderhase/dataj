package de.nikem.dataj.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.nikem.dataj.ListPage;

public class PaginationPanel extends JPanel {
	private static final long serialVersionUID = 8625370517088129864L;
	
	//.*"(.*)"\s*\:\s*"(.*)".*
	static final Pattern PATTERN = Pattern.compile(".*\"(.*)\"\\s*\\:\\s*\"(.*)\".*");
	
	private JLabel textLabel;
	private JButton previousButton;
	private JButton nextButton;
	private ResourceBundle texts;
	private final ServerTableRowSorter sorter;
	
	public PaginationPanel(ServerTableRowSorter sorter) {
		super(new BorderLayout());
		this.sorter = sorter;
		init();
	}

	protected void init() {
		texts = DatajResourceBundle.getBundle(getLocale());
		
		add(getTextLabel(), BorderLayout.WEST);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(getPreviousButton());
		buttonPanel.add(getNextButton());
		add(buttonPanel, BorderLayout.EAST);
	}
	
	public JLabel getTextLabel() {
		if (textLabel == null) {
			textLabel = new JLabel();
		}
		return textLabel;
	}
	
	public JButton getPreviousButton() {
		if (previousButton == null) {
			previousButton = new JButton();
			previousButton.setText(getTexts().getString("sPrevious"));
			previousButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					sorter.previous();
				}
			});
		}
		return previousButton;
	}
	
	public JButton getNextButton() {
		if (nextButton == null) {
			nextButton = new JButton();
			nextButton.setText(getTexts().getString("sNext"));
			nextButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					sorter.next();
				}
			});
		}
		return nextButton;
	}
	
	public ServerTableRowSorter getSorter() {
		return sorter;
	}
	
	/**
	 * Update the displayed text
	 * @param listPage
	 */
	public void update(ListPage<?> listPage) {
		if (listPage == null) {
			getTextLabel().setText(getTexts().getString("sInfoEmpty"));
		} else {
			String text = getTexts().getString("sInfo");
			text = text.replaceAll("_START_", Integer.toString(sorter.getiDisplayStart() + 1));
			text = text.replaceAll("_END_", Integer.toString(sorter.getiDisplayStart() + listPage.getData().size()));
			text = text.replaceAll("_TOTAL_", Integer.toString(listPage.getTotalDisplayRecords()));
			getTextLabel().setText(text);
		}
		
		getNextButton().setEnabled(listPage != null && sorter.getiDisplayStart() + listPage.getData().size() < listPage.getTotalDisplayRecords());
		getPreviousButton().setEnabled(listPage != null && sorter.getiDisplayStart() > 1);
	}
	
	public ResourceBundle getTexts() {
		return texts;
	}
	
	public void setTexts(ResourceBundle texts) {
		this.texts = texts;
	}

	public static void main(String[] args) {
		ServerTableRowSorter sorter = new ServerTableRowSorter();
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new PaginationPanel(sorter));
		frame.pack();
		frame.setVisible(true);
	}

}
