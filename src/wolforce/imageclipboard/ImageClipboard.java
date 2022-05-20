package wolforce.imageclipboard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
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
			System.out.println("reading: " + path);
			try {
				addImage(path, ImageIO.read(new File(path)));
				filePaths.add(path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		writePrefs();

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
		}

		@Override
		public void paint(Graphics g) {
			g.setColor(new Color(54, 57, 63));
			g.fillRect(0, 0, getWidth(), getHeight());

			if (image != null) {
				g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
			}
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
					setClipboard(i, path);
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

		new Thread(() -> {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
			}
			imgsPanel.invalidate();
			frame.pack();
			refreshLayout();
			imgsPanel.revalidate();
			imgsPanel.repaint();
		}).start();
		imgsPanel.invalidate();
		imgsPanel.add(button);
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
//	public static void writeToClipboard(Image image) {
//		try {
//			if (image == null)
//				throw new IllegalArgumentException("Image can't be null");
//
//			BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
//			Graphics2D g2 = buffered.createGraphics();
//			g2.setColor(Color.WHITE);
//			g2.fillRect(0, 0, w, h);
//			g2.drawImage(image, 0, 0, w, h, null);
//			Thread.sleep(100);
//			ImageTransferable transferable = new ImageTransferable(buffered);
//			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	public static void getBufferedImage(Image image, Consumer<Image> imageConsumer) {

		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = buffered.createGraphics();
		g2.setColor(new Color(0, 0, 0, 0));
		g2.fillRect(0, 0, w, h);
		if (g2.drawImage(image, 0, 0, w, h, (img2, info2, x2, y2, w2, h2) -> {
			if (info2 == ImageObserver.ALLBITS) {
				g2.dispose();
				imageConsumer.accept(img2);
				return false;
			}
			return true;
		})) {
			g2.dispose();
			imageConsumer.accept(buffered);
		}
	}

	public static void setClipboard(Image image, String path) {
//		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(image), null);
//		getBufferedImage(image, (buffered) -> {
//			ImageSelection imgSel = new ImageSelection(buffered);
//			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
//		});

		File file = new File(path);
		List<File> listOfFiles = new ArrayList<File>();
		listOfFiles.add(file);

		FileTransferable<File> ft = new FileTransferable<File>(listOfFiles);

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ft, new ClipboardOwner() {
			@Override
			public void lostOwnership(Clipboard clipboard, Transferable contents) {
				System.out.println("Lost ownership");
			}
		});
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

	public static class FileTransferable<T> implements Transferable {

		private List<T> listOfFiles;

		public FileTransferable(List<T> listOfFiles) {
			this.listOfFiles = listOfFiles;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.javaFileListFlavor };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.javaFileListFlavor.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			return listOfFiles;
		}
	}

}
