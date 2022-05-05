package wolforce.imageclipboard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
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
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

public class ImageClipboard {

	static int w = 1, h = 1;
	static JFrame frame;
	static Container imgsPanel;
	static GridLayout layout;
	static LinkedList<String> filePaths = new LinkedList<>();
	final static String PREF_NAME = "filelist";

	public static void main(String[] args) throws IOException {

		frame = new JFrame("Image Clipboard");
//		frame.setUndecorated(true);
//		frame.setBackground(new Color(0, 0, 0, 0));
		frame.setPreferredSize(new Dimension(400, 400));
		frame.setIconImage(ImageIO.read(ClassLoader.getSystemResource("icon.png")));
		frame.setDropTarget(new DropTarget(frame, new DropTargetHandler()));

		Container pane = frame.getContentPane();
		pane.setLayout(new BorderLayout(10, 10));

		imgsPanel = new Container();
		layout = new GridLayout();
		imgsPanel.setLayout(layout);
		pane.add(imgsPanel, BorderLayout.CENTER);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		for (String path : readPrefs()) {
			filePaths.add(path);
			System.out.println("reading: " + path);
			try {
				addImage(path, ImageIO.read(new File(path)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		refreshLayout();
	}

	private static void refreshLayout() {
		int listSize = filePaths.size();
		w = 1;
		h = 1;
		while (w * h < listSize) {
			if ((w + 1) * h == listSize)
				w++;
			else if (w > h)
				h++;
			else
				w++;
		}
		layout.setColumns(w);
		layout.setRows(h);
	}

	static class MyButton extends JPanel {

		private static final long serialVersionUID = 1L;

		private Image image;

		public MyButton(Image image) {
			this.image = image;
			setBackground(new Color(0, 0, 0, 0));
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			g.clearRect(0, 0, getWidth(), getHeight());

			double w1 = image.getWidth(null);
			double h1 = image.getHeight(null);
			double w2 = imgsPanel.getWidth() / w;
			double h2 = imgsPanel.getHeight() / h;

			double f1 = w1 > h1 ? h1 / w1 : w1 / h1;
			double f2 = w2 > h2 ? h2 : w2;

			g.drawImage(image, 0, 0, (int) (f1 * f2), (int) (f1 * f2), null);
		}
	}

	static void addImage(final String path, final Image i) {

//		BufferedImage i = new BufferedImage(_i.getWidth(null), _i.getHeight(null), BufferedImage.TYPE_INT_ARGB);
//		i.getGraphics().drawImage(_i, 0, 0, frame);

		final MyButton button = new MyButton(i);

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
						refreshLayout();
						imgsPanel.revalidate();
						imgsPanel.repaint();
					});
					menu.add(removeItem);
					menu.show(e.getComponent(), e.getX(), e.getY());
					break;
				}
			}
		});

		imgsPanel.add(button);
		refreshLayout();
		imgsPanel.revalidate();
		imgsPanel.repaint();
		frame.pack();
		refreshLayout();
		imgsPanel.revalidate();
		imgsPanel.repaint();
	}

	static String[] readPrefs() {
		Preferences prefs = Preferences.userNodeForPackage(wolforce.imageclipboard.ImageClipboard.class);
		String s = prefs.get(PREF_NAME, "");
		if (s.startsWith(";"))
			s = s.substring(1);
		if (s.endsWith(";"))
			s = s.substring(-1);
		return s.split(";");
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
					refreshLayout();
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
