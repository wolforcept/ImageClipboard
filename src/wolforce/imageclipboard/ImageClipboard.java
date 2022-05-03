package wolforce.imageclipboard;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

public class ImageClipboard {

	static JFrame frame;
	static JPanel imgsPanel;
	static GridLayout layout;
	static LinkedList<String> filePaths = new LinkedList<>();
	final static String PREF_NAME = "filelist";

	public static void main(String[] args) throws IOException {

		frame = new JFrame("Image Clipboard");
		frame.setPreferredSize(new Dimension(400, 400));
		frame.setIconImage(ImageIO.read(ClassLoader.getSystemResource("icon.png")));
		frame.setDropTarget(new DropTarget(frame, new DropTargetHandler()));

		Container pane = frame.getContentPane();
		pane.setLayout(new BorderLayout(10, 10));

		imgsPanel = new JPanel();
		layout = new GridLayout();
		imgsPanel.setLayout(layout);
		pane.add(imgsPanel, BorderLayout.CENTER);

		for (String path : readPrefs()) {
			filePaths.add(path);
			System.out.println("reading: " + path);
			try {
				addImage(path, ImageIO.read(new File(path)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}
	
	private void refreshLayout() {
		layout.get
	}

	static void addImage(final String path, final Image i) {

//		BufferedImage i = new BufferedImage(_i.getWidth(null), _i.getHeight(null), BufferedImage.TYPE_INT_ARGB);
//		i.getGraphics().drawImage(_i, 0, 0, frame);

		final JButton button = new JButton(new ImageIcon(i));

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				switch (e.getButton()) {
				case MouseEvent.BUTTON1:
					frame.setState(JFrame.ICONIFIED);
					setClipboard(i);
					break;
				case MouseEvent.BUTTON3:
					JPopupMenu menu = new JPopupMenu();
					JMenuItem removeItem = new JMenuItem("Remove");
					removeItem.addActionListener(ev -> {
						imgsPanel.remove(button);
						filePaths.remove(path);
						writePrefs();
						frame.revalidate();
						frame.repaint();
					});
					menu.add(removeItem);
					menu.show(e.getComponent(), e.getX(), e.getY());
					break;
				}
			}
		});

		imgsPanel.add(button);
		frame.revalidate();
		frame.pack();
	}

	static String[] readPrefs() {
		Preferences prefs = Preferences.userNodeForPackage(wolforce.imageclipboard.ImageClipboard.class);
		return prefs.get(PREF_NAME, "").split(";");
	}

	static void writePrefs() {
		Preferences prefs = Preferences.userNodeForPackage(wolforce.imageclipboard.ImageClipboard.class);
		prefs.put(PREF_NAME, filePaths.stream().collect(Collectors.joining(";")));
	}

	static class DropTargetHandler extends DropTargetAdapter {

		@Override
		public void drop(DropTargetDropEvent dtde) {

			Transferable transferable = dtde.getTransferable();
			dtde.acceptDrop(dtde.getDropAction());
			try {

				@SuppressWarnings("unchecked")
				List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				for (File file : files) {
					Image image = ImageIO.read(file);
					String path = file.getAbsolutePath();
					addImage(path, image);
					filePaths.add(path);
					writePrefs();
				}
				dtde.dropComplete(true);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Retrieve an image from the system clipboard.
	 *
	 * @return the image from the clipboard or null if no image is found
	 */
	public static Image readFromClipboard() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
				Image image = (Image) t.getTransferData(DataFlavor.imageFlavor);
				return image;
			}
		} catch (Exception e) {
		}

		return null;
	}

	/**
	 * Place an image on the system clipboard.
	 *
	 * @param image - the image to be added to the system clipboard
	 */
	public static void writeToClipboard(Image image) {
		try {
			if (image == null)
				throw new IllegalArgumentException("Image can't be null");

			ImageTransferable transferable = new ImageTransferable(image);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// code below from exampledepot.com
	// This method writes a image to the system clipboard.
	// otherwise it returns null.
	public static void setClipboard(Image image) {
		ImageSelection imgSel = new ImageSelection(image);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
	}

	// This class is used to hold an image while on the clipboard.
	static class ImageSelection implements Transferable {
		private Image image;

		public ImageSelection(Image image) {
			this.image = image;
		}

		// Returns supported flavors
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}

		// Returns true if flavor is supported
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}

		// Returns image
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (!DataFlavor.imageFlavor.equals(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return image;
		}
	}

	static class ImageTransferable implements Transferable {
		private Image image;

		public ImageTransferable(Image image) {
			this.image = image;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (isDataFlavorSupported(flavor)) {
				return image;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor == DataFlavor.imageFlavor;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}
	}

}
