package some.eclipse.control.browser.views;

import java.io.Console;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.internal.browser.WebBrowserEditor;
import org.eclipse.ui.internal.browser.WebBrowserEditorInput;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.eclipse.core.runtime.IAdaptable;
import javax.inject.Inject;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class SoMeControlView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "some.eclipse.control.browser.views.SoMeControlView";

	@Inject IWorkbench workbench;
	
	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private Action action1;
	private Action action2;
	private Action doubleClickAction;
	private WebBrowserEditorInput edtInput;
	private int debugLine = 0;
	 
	class TreeObject implements IAdaptable {
		private String name;
		private TreeParent parent;
		
		public TreeObject(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setParent(TreeParent parent) {
			this.parent = parent;
		}
		public TreeParent getParent() {
			return parent;
		}
		@Override
		public String toString() {
			return getName();
		}
		@Override
		public <T> T getAdapter(Class<T> key) {
			return null;
		}
	}
	
	class TreeParent extends TreeObject {
		private ArrayList children;
		public TreeParent(String name) {
			super(name);
			children = new ArrayList();
		}
		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}
		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}
		public TreeObject [] getChildren() {
			return (TreeObject [])children.toArray(new TreeObject[children.size()]);
		}
		public boolean hasChildren() {
			return children.size()>0;
		}
	}

	class ViewContentProvider implements ITreeContentProvider {
		private TreeParent invisibleRoot;

		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				if (invisibleRoot==null) initialize();
				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject)child).getParent();
			}
			return null;
		}
		public Object [] getChildren(Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent)parent).getChildren();
			}
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent)
				return ((TreeParent)parent).hasChildren();
			return false;
		}
/*
 * We will set up a dummy model to initialize tree heararchy.
 * In a real code, you will connect to a real model and
 * expose its hierarchy.
 */
		private void initialize() {
			TreeObject to1 = new TreeObject("Take Control of Internal Browser");
			TreeObject to2 = new TreeObject("Leaf 2");
			TreeObject to3 = new TreeObject("Leaf 3");
			TreeParent p1 = new TreeParent("Browser");
			p1.addChild(to1);
			p1.addChild(to2);
			p1.addChild(to3);
			
			TreeObject to4 = new TreeObject("Leaf 4");
			TreeParent p2 = new TreeParent("Parent 2");
			p2.addChild(to4);
			
			TreeParent root = new TreeParent("SoMe");
			root.addChild(p1);
			root.addChild(p2);
			
			invisibleRoot = new TreeParent("");
			invisibleRoot.addChild(root);
		}
	}

	class ViewLabelProvider extends LabelProvider {

		public String getText(Object obj) {
			return obj.toString();
		}
		public Image getImage(Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			if (obj instanceof TreeParent)
			   imageKey = ISharedImages.IMG_OBJ_FOLDER;
			return workbench.getSharedImages().getImage(imageKey);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		
	viewer.setContentProvider(new ViewContentProvider());
	viewer.setInput(getViewSite());
	viewer.setLabelProvider(new ViewLabelProvider());

		// Create the help context id for the viewer's control
		workbench.getHelpSystem().setHelp(viewer.getControl(), "some.eclipse.control.browser.viewer");
		getSite().setSelectionProvider(viewer);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				SoMeControlView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		action1 = new Action() {
			public void run() {
				enableBrowserIntelliSense();
			}
		};
		action1.setText("Take Control of Internal Browser");
		action1.setToolTipText("Take Control of Internal Browser");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(workbench.getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = viewer.getStructuredSelection();
				Object obj = selection.getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
				switch (obj.toString()) {
				case "1":
					
					break;

				default:
					break;
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"SoMe Control Panel",
			message);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	private String getFileAbsolutePath(IEditorInput input) {
		String absPath = "about:blank";
		System.out.println("getFileAbsolutePath: "+input.getClass().getName()+" "+input.getName());
		if (input instanceof FileStoreEditorInput) {
			System.out.println("The file belongs to FileStoreEditorInput");
			URI file = ((FileStoreEditorInput)input).getURI();
			absPath = file.getPath();
		}
		else if(input instanceof IFileEditorInput){
			System.out.println("The file belongs to IFileEditorInput");
			try {
				URI file = ((IFileEditorInput)input).getFile().getLocationURI();
				absPath = file.getRawPath();
			}
			catch (Exception e) {
				System.out.println(e.toString());
			}
		}
		return absPath;
	}
	
//	private IEditorPart getInternalBrowser() {
//		IEditorPart result = null;
//		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
//			for ( IWorkbenchPage p : window.getPages() ) {
//				for ( IEditorPart edt : p.getEditors() ) {
//					if ( edt.getClass().getName().equals("org.eclipse.ui.internal.browser.WebBrowserEditor") ) {
//						result = edt;
//					}
//				}
//			}
//		}
//		return result;
//	}
	
	private void debugActivePart(String flag, IWorkbenchPart activePart) {
		System.out.println(debugLine+" "+flag+" - "+activePart.getClass().getName());
		debugLine ++;
	}
	
	private void enableBrowserIntelliSense() {
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			try {
				IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
				try {
					IWorkbenchPage page = activeWindow.getActivePage();
					try {
						IEditorPart activeEditor = page.getActiveEditor();
						page.addPartListener(new IPartListener() {
							
							@Override
							public void partOpened(IWorkbenchPart activePart) {
								debugActivePart("partDeactivated: ", activePart);
							}
							
							@Override
							public void partDeactivated(IWorkbenchPart activePart) {
								debugActivePart("partDeactivated: ", activePart);
							}
							
							@Override
							public void partClosed(IWorkbenchPart activePart) {
								debugActivePart("partClosed: ", activePart);
							}
							
							@Override
							public void partBroughtToTop(IWorkbenchPart activePart) {
								debugActivePart("partBroughtToTop: ", activePart);
							}
							
							@Override
							public void partActivated(IWorkbenchPart activePart) {
								debugActivePart("partActivated: ", activePart);
								// 仅在编辑器间跳转时执行刷新浏览器动作
								if ( activePart.getClass().getName().equals(activeEditor.getClass().getName()) & ( ! ( activeEditor instanceof WebBrowserEditor ) ) ) {
									System.out.println("您激活了文本编辑器，现在为您刷新浏览器");
									try {
										IEditorInput input = activeEditor.getEditorInput();
										System.out.println("inside: "+getFileAbsolutePath(input));
										
										String pathWithProtocol = "file:"+getFileAbsolutePath(input);
										System.out.println("即将访问："+pathWithProtocol);
										edtInput = new WebBrowserEditorInput(new URL(pathWithProtocol));
										System.out.println("即将访问："+edtInput.getURL().toString());
										if ( edtInput.getURL().toString().equals(pathWithProtocol) ) {
											if ( pathWithProtocol.endsWith(".html") ) {
												autoRefreshBrowser(pathWithProtocol);
											}
										}
									}
									catch (Exception e) {
										System.out.println("There's no editor input! "+e.toString());
									}
								}
							}
						});
						String editorPackage = activeEditor.getClass().getName();
						System.out.println("Taking control of "+editorPackage+": "+activeEditor.getTitle());
					}
					catch (Exception e) {
						System.out.println("There's no active editor!");
					}
				}
				catch (Exception e) {
					System.out.println("There's no active page!");
				}
			}
			catch (Exception e) {
				System.out.println("There's no active window!");
			}
		}
		catch (Exception e) {
			System.out.println("There's no workbench!");
		}
	}
	
	private void autoRefreshBrowser(String pathWithProtocol) {
		try {
			System.out.println("准备访问："+pathWithProtocol);
			URL pathURL = new URL(pathWithProtocol);
			edtInput = new WebBrowserEditorInput(pathURL);
			WebBrowserEditor.open(edtInput);
		}
		catch (Exception e) {
			System.out.println("非法URL");
		}
	}
}
