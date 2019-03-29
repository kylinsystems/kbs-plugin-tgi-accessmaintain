package org.tgi.webui.apps.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.ToolBarButton;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WStringEditor;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.MClient;
import org.compiere.model.MColumn;
import org.compiere.model.MForm;
import org.compiere.model.MFormAccess;
import org.compiere.model.MInfoWindow;
import org.compiere.model.MInfoWindowAccess;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessAccess;
import org.compiere.model.MRole;
import org.compiere.model.MWindow;
import org.compiere.model.MWindowAccess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.North;
/**
 * Help to maintain access to windows, reports, forms, ...
 * @author nmicoud, TGI
 */

public class WAccessMaintain extends ADForm implements EventListener<Event>, WTableModelListener {

	private static final long serialVersionUID = -8528918348159236707L;
	private Properties m_ctx = null;
	private Listbox fType, fObject, fRoleType, fActionType;
	private WStringEditor fFilterObject = new WStringEditor();
	private WStringEditor fFilterRole = new WStringEditor();
	private WStringEditor fFilterClient = new WStringEditor();
	private WDateEditor fCreatedFrom = new WDateEditor();
	private Button bProcess = new Button();
	private WListbox table;
	private Vector<Vector<Object>> data;
	private ListModelTable tableModel;
	private ToolBarButton bSelect = new ToolBarButton("SelectAll");

	final static String roleType_all = "1";
	final static String roleType_withAccess = "2";
	final static String roleType_withoutAccess = "3";
	final static String roleType_withAccessActive = "4";

	final static String actionType_add = "1";
	final static String actionType_del = "2";
	final static String actionType_activate = "3";
	final static String actionType_deactivate = "4";

	private int idxColumnSelect = 0;
	private int idxColumnRole = 2;
	private int idxColumnExists = 3;
	private int idxColumnActive = 4;
	private boolean checkAllSelected = true;

	protected void initForm() {
		m_ctx = Env.getCtx();
		try {
			dynInit();
			zkInit();
		}
		catch (Exception ex) {
		}
	}

	private void zkInit() throws Exception {
		this.setHeight("100%");

		Borderlayout mainLayout = new Borderlayout();
		mainLayout.setStyle("width: 100%; height: 100%; position: absolute;");
		appendChild(mainLayout);

		North north = new North();
		mainLayout.appendChild(north);
		ZKUpdateUtil.setVflex(north, "1");

		Grid grid = GridFactory.newGridLayout();
		grid.setHflex("1");

		Columns columns = new Columns();
		grid.appendChild(columns);

		Column column = new Column();
		column.setWidth("20%");
		columns.appendChild(column);
		column = new Column();
		column.setWidth("80%");
		columns.appendChild(column);

		north.appendChild(grid);

		Rows rows = grid.newRows();
		Row row = rows.newRow();

		Hbox hb = new Hbox();
		hb.appendChild(new Label("Type"));
		hb.appendChild(fType);
		hb.appendChild(fFilterObject.getComponent());
		hb.appendChild(fCreatedFrom.getComponent());
		hb.appendChild(fObject);
		row.appendCellChild(hb, 2);

		row = rows.newRow();
		hb = new Hbox();
		hb.appendChild(fRoleType);
		hb.appendChild(fFilterClient.getComponent());
		hb.appendChild(fFilterRole.getComponent());
		row.appendCellChild(hb, 2);

		row = rows.newRow();
		hb = new Hbox();
		hb.appendChild(bSelect);
		hb.appendChild(new Label("Action :"));
		hb.appendChild(fActionType);
		hb.appendChild(bProcess);
		row.appendCellChild(hb, 2);

		Center center = new Center();
		mainLayout.appendChild(center);
		center.setAutoscroll(true);

		ZKUpdateUtil.setVflex(table, "1");
		ZKUpdateUtil.setHflex(table, "2");
		center.appendChild(table);
	}

	public void dynInit()  {		
		fType = new Listbox();
		fType.setMold("select");
		fType.addEventListener(Events.ON_SELECT, this);

		fType.appendItem("", "");
		fType.appendItem(Msg.getElement(m_ctx, "AD_Window_ID"), MMenu.ACTION_Window);
		fType.appendItem(Msg.getElement(m_ctx, "AD_Process_ID"), MMenu.ACTION_Process);
		fType.appendItem(Msg.getCleanMsg(m_ctx, "Report"), MMenu.ACTION_Report);
		fType.appendItem(Msg.getElement(m_ctx, "AD_Form_ID"), MMenu.ACTION_Form);
		fType.appendItem(Msg.getElement(m_ctx, "AD_InfoWindow_ID"), MMenu.ACTION_Info);

		fObject = new Listbox();
		fObject.setMold("select");
		fObject.addEventListener(Events.ON_SELECT, this);

		fRoleType = new Listbox();
		fRoleType.setMold("select");
		fRoleType.addEventListener(Events.ON_SELECT, this);
		fRoleType.appendItem(Msg.getMsg(m_ctx, "AccessMaintain_AllRoles"), roleType_all);
		fRoleType.appendItem(Msg.getMsg(m_ctx, "AccessMaintain_RolesWithAccess"), roleType_withAccess);
		fRoleType.appendItem(Msg.getMsg(m_ctx, "AccessMaintain_RolesWithoutAccess"), roleType_withoutAccess);
		fRoleType.appendItem(Msg.getMsg(m_ctx, "AccessMaintain_RolesWithActiveAccess"), roleType_withAccessActive);

		fActionType = new Listbox();
		fActionType.setMold("select");
		fActionType.addEventListener(Events.ON_SELECT, this);
		fActionType.appendItem(Msg.getMsg(m_ctx, "AccessMaintain_Insert"), "1");
		fActionType.appendItem(Msg.getMsg(m_ctx, "AccessMaintain_Delete"), "2");
		fActionType.appendItem(Msg.getMsg(m_ctx, "AccessMaintain_Activate"), "3");
		fActionType.appendItem(Msg.getMsg(m_ctx, "AccessMaintain_Deactivate"), "4");

		fFilterClient.getComponent().addEventListener(Events.ON_BLUR, this);
		fFilterRole.getComponent().addEventListener(Events.ON_BLUR, this);
		fFilterObject.getComponent().addEventListener(Events.ON_BLUR, this);
		fFilterClient.getComponent().addEventListener(Events.ON_OK, this);
		fFilterRole.getComponent().addEventListener(Events.ON_OK, this);
		fFilterObject.getComponent().addEventListener(Events.ON_OK, this);
		fFilterObject.getComponent().setPlaceholder(Msg.getMsg(m_ctx, "AccessMaintain_FiltreName"));
		fFilterRole.getComponent().setPlaceholder(Msg.getMsg(m_ctx, "AccessMaintain_FilterRole"));
		fFilterClient.getComponent().setPlaceholder(Msg.getMsg(m_ctx, "AccessMaintain_FilterTenant"));
		fCreatedFrom.getComponent().addEventListener(Events.ON_OK, this);
		fCreatedFrom.getComponent().addEventListener(Events.ON_BLUR, this);
		fCreatedFrom.getComponent().setPlaceholder(Msg.getMsg(m_ctx, "AccessMaintain_CreatedSince"));

		bSelect.setMode("toggle");
		bSelect.setImage(ThemeManager.getThemeResource("images/SelectAll24.png"));
		bSelect.setTooltiptext(Util.cleanAmp(Msg.getMsg(Env.getCtx(), "SelectAll")));
		bSelect.addEventListener(Events.ON_CLICK, this);
		bProcess.setImage(ThemeManager.getThemeResource("images/Process16.png"));
		bProcess.addEventListener(Events.ON_CLICK, this);

		initTable();
	}

	void initTable() {
		data = new Vector<Vector<Object>>();
		table = ListboxFactory.newDataTable();

		ColumnInfo[] layout = {
				new ColumnInfo("", "Select", IDColumn.class, false, false, ""),
				new ColumnInfo(Msg.getElement(Env.getCtx(), "AD_Client_ID"), "Tenant", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "AD_Role_ID"), "Role", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "AccessMaintain_Exists"), "Exists", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "IsActive"), "IsActive", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "IsReadWrite"), "IsReadWrite", String.class)
		};

		table.prepareTable(layout, "", "", true , "osef");
		table.addEventListener(Events.ON_SELECT, this);
		table.setSizedByContent(true);
		tableModel = new ListModelTable(data);
		table.setModel(tableModel);
		table.repaint();
	}

	public void onEvent(Event event) throws Exception {
		Object source = event.getTarget();

		if (source == fType) {
			String type = getSelectedValue(fType);
			if (!Util.isEmpty(type))
				updateObjectList(type);
		}
		else if (source == fFilterClient.getComponent()) {
			if (event.getName().equals(Events.ON_BLUR) || event.getName().equals(Events.ON_OK))
				updateTableContent();
		}
		else if (source == fFilterRole.getComponent()) {
			if (event.getName().equals(Events.ON_BLUR) || event.getName().equals(Events.ON_OK))
				updateTableContent();
		}
		else if (source == fFilterObject.getComponent()) {
			if (event.getName().equals(Events.ON_BLUR) || event.getName().equals(Events.ON_OK)) {
				String type = getSelectedValue(fType);
				if (!Util.isEmpty(type))
					updateObjectList(getSelectedValue(fType));
			}
		}
		else if (source == fCreatedFrom.getComponent()) {
			if (event.getName().equals(Events.ON_BLUR) || event.getName().equals(Events.ON_OK)) {
				String type = getSelectedValue(fType);
				if (!Util.isEmpty(type))
					updateObjectList(getSelectedValue(fType));
			}
		}
		else if (source == fRoleType) {
			updateTableContent();
		}
		else if (source == fObject) {
			updateTableContent();
		}
		else if (source == fActionType) {
			bProcess.setLabel(getSelectedLabel(fActionType));
		}
		else if (source == bProcess) {
			process();
		}
		else if (event.getTarget().equals(bSelect))
			onbSelect();
		else if (event.getTarget().equals(table)) {
			if (table.getRowCount() > 0 && table.getSelectedRow() >= 0) {
				boolean isChecked = (Boolean) table.getValueAt(table.getSelectedRow(), 0);
				table.getModel().setValueAt(!isChecked, table.getSelectedRow(), 0);
			}
		}
	}

	String getSelectedValue(Listbox lb) {
		Listitem listitem = lb.getSelectedItem();
		if (listitem != null) {
			String value = listitem.getValue();
			if (!Util.isEmpty(value))
				return value;
		}
		return "";
	}

	String getSelectedLabel(Listbox lb) {
		Listitem listitem = lb.getSelectedItem();
		if (listitem != null) {
			String label = listitem.getLabel();
			if (!Util.isEmpty(label))
				return label;
		}
		return "";
	}

	int getObjectID() {
		Listitem listitem = fObject.getSelectedItem();
		if (listitem != null)
			return listitem.getValue();
		return -1;
	}

	String getObjectname(String type) {
		if (type.equals(MMenu.ACTION_Window))
			return "Window";
		else if (type.equals(MMenu.ACTION_Process) || type.equals(MMenu.ACTION_Report))
			return "Process";
		else if (type.equals(MMenu.ACTION_Form))
			return "Form";
		else if (type.equals(MMenu.ACTION_Info))
			return "InfoWindow";
		return "";
	}

	String getTablename(String type) {
		return "AD_" + getObjectname(type);
	}

	void updateObjectList(String type) {
		fObject.removeAllItems();
		fObject.appendItem("", -1);

		String objectName = fFilterObject.isNullOrEmpty() ? "" : (String) fFilterObject.getValue();
		String tablename = getTablename(type);
		boolean isAccessAdv = MRole.getDefault().isAccessAdvanced();
		boolean isBaseLang = Env.isBaseLanguage(Env.getLoginLanguage(m_ctx), tablename);
		Timestamp createdFrom = fCreatedFrom.isNullOrEmpty() ? null : (Timestamp) fCreatedFrom.getValue();

		String name = "o.Name";
		if (!isBaseLang) {
			if (isAccessAdv)
				name = "o.Name || ' (' || t.Name || ') [' || o." + tablename + "_ID || ']'";
			else
				name = "t.Name";
		}

		StringBuilder sql = new StringBuilder("SELECT o.").append(tablename).append("_ID, ").append(name).append(" FROM ").append(tablename).append(" o");

		if (!isBaseLang)
			sql.append(" INNER JOIN ").append(tablename).append("_Trl t ON (o.").append(tablename).append("_ID = t.").append(tablename).append("_ID AND t.AD_Language =").append(DB.TO_STRING(Env.getLoginLanguage(m_ctx).getAD_Language())).append(")");

		if (!isAccessAdv)
			sql.append(" INNER JOIN ").append(tablename).append("_Access a ON (o.").append(tablename).append("_ID = a.").append(tablename).append("_ID AND a.AD_Role_ID = ").append(Env.getAD_Role_ID(m_ctx)).append(" AND a.IsActive = 'Y')"); 

		sql.append(" WHERE o.IsActive = 'Y'");

		if (type.equals(MMenu.ACTION_Process))
			sql.append(" AND IsReport = 'N'");
		else if (type.equals(MMenu.ACTION_Report))
			sql.append(" AND IsReport = 'Y'");

		if (!Util.isEmpty(objectName)) {
			if (isBaseLang)
				sql.append(" AND UPPER(Name) LIKE UPPER('%").append(objectName).append("%')");
			else
				sql.append(" AND (UPPER(o.Name) LIKE UPPER('%").append(objectName).append("%') OR (UPPER(t.Name) LIKE UPPER('%").append(objectName).append("%')))");
		}

		if (createdFrom != null)
			sql.append(" AND o.Created >=").append(DB.TO_DATE(createdFrom));

		sql.append(" ORDER BY 2");

		for (KeyNamePair knp : DB.getKeyNamePairs(sql.toString(), false))
			fObject.appendItem(knp.getName(), knp.getKey());
	}

	void updateTableContent() {
		table.clearTable();
		data.removeAllElements();

		if (getObjectID() <= 0)
			return;

		String tablename = getTablename(getSelectedValue(fType));
		boolean hasReadWrite = MColumn.get(m_ctx, tablename + "_Access", "IsReadWrite") != null;
		String clientName = fFilterClient.isNullOrEmpty() ? "" : (String) fFilterClient.getValue();
		String roleName = fFilterRole.isNullOrEmpty() ? "" : (String) fFilterRole.getValue();

		StringBuilder sql = new StringBuilder("SELECT r.AD_Client_ID, r.AD_Role_ID, a.IsActive");

		if (hasReadWrite)
			sql.append(", a.IsReadWrite");

		sql.append(" FROM AD_Client c, AD_Role r")
		.append(" LEFT OUTER JOIN ").append(tablename).append("_Access a ON (r.AD_Role_ID = a.AD_Role_ID AND a.").append(tablename).append("_ID = ").append(getObjectID()).append(")")
		.append(" WHERE r.AD_Client_ID = c.AD_Client_ID")
		.append(" AND c.IsActive = 'Y' AND r.IsActive = 'Y'");

		if (Env.getAD_Client_ID(m_ctx) > 0)
			sql.append(" AND c.AD_Client_ID = ").append(Env.getAD_Client_ID(m_ctx));

		if (!Util.isEmpty(clientName))
			sql.append(" AND UPPER(c.Name) LIKE UPPER('%").append(clientName).append("%')");
		if (!Util.isEmpty(roleName))
			sql.append(" AND UPPER(r.Name) LIKE UPPER('%").append(roleName).append("%')");

		sql.append(" ORDER BY c.Name, r.Name");

		String roleType = getSelectedValue(fRoleType);
		List<List<Object>> rows = DB.getSQLArrayObjectsEx(null, sql.toString());
		if (rows != null && rows.size() > 0) {
			for (List<Object> row : rows) {
				int clientID = ((BigDecimal) row.get(0)).intValue();
				int roleID = ((BigDecimal) row.get(1)).intValue();
				boolean isExists = row.get(2) != null;
				boolean isActive = row.get(2) != null ? ((String) row.get(2)).equals("Y") : false;
				boolean isReadWrite = hasReadWrite && row.get(3) != null ? ((String) row.get(3)).equals("Y") : false;

				if (!Util.isEmpty(roleType)) {
					if (roleType.equals(roleType_withAccess) && !isExists)
						continue;
					if (roleType.equals(roleType_withoutAccess) && isExists)
						continue;
					if (roleType.equals(roleType_withAccessActive) && (!isExists || !isActive))
						continue;
				}

				Vector<Object> line = new Vector<Object>();
				data.add(line);

				line.add(false);

				KeyNamePair vnp = new KeyNamePair(clientID, MClient.get(m_ctx, clientID).getName());				
				line.add(vnp);

				vnp = new KeyNamePair(roleID, MRole.get(m_ctx, roleID).getName());				
				line.add(vnp);

				line.addElement(isExists);
				line.addElement(isActive);
				line.addElement(isReadWrite);

			}
		}

		tableModel = new ListModelTable(data);
		table.setModel(tableModel);
		table.repaint();				
	}

	void process() {

		String actionType = getSelectedValue(fActionType);
		String objectType = getSelectedValue(fType);
		String tableName = getTablename(objectType);
		int objectID = getObjectID();

		if (!Util.isEmpty(actionType)) {

			for (int i = 0; i < table.getRowCount(); i++) {
				if (((Boolean) table.getValueAt(i, idxColumnSelect)).booleanValue()) {
					KeyNamePair pp = (KeyNamePair) table.getValueAt(i, idxColumnRole);

					int roleID = pp.getKey();
					StringBuilder sql = new StringBuilder("");

					if (!MRole.getDefault().isAccessAdvanced() && MRole.get(m_ctx, roleID).isAccessAdvanced())
						continue;

					if (actionType.equals(actionType_add) && !(((Boolean) table.getValueAt(i, idxColumnExists)).booleanValue())) {

						if (objectType.equals(MMenu.ACTION_Window)) { // Voir restriction sur System (aucun intérêt de proposer Process, Window, ... aux rôles sociétés)
							MWindowAccess wa = new MWindowAccess(MWindow.get(m_ctx, objectID), roleID);
							wa.saveEx();
						}
						else if (objectType.equals(MMenu.ACTION_Process) || objectType.equals(MMenu.ACTION_Report)) {
							MProcessAccess wa = new MProcessAccess(MProcess.get(m_ctx, objectID), roleID);
							wa.saveEx();
						}
						else if (objectType.equals(MMenu.ACTION_Form)) {
							MFormAccess wa = new MFormAccess(new MForm(m_ctx, objectID, null), roleID);
							wa.saveEx();
						}
						else if (objectType.equals(MMenu.ACTION_Info)) {
							MInfoWindowAccess wa = new MInfoWindowAccess(new MInfoWindow(m_ctx, objectID, null), roleID);
							wa.saveEx();
						}
					}
					else {
						if (actionType.equals(actionType_del) && (((Boolean) table.getValueAt(i, idxColumnExists)).booleanValue()))
							sql.append("DELETE FROM ").append(tableName).append("_Access");
						else if (actionType.equals(actionType_activate) && !(((Boolean) table.getValueAt(i, idxColumnActive)).booleanValue()))
							sql.append("UPDATE ").append(tableName).append("_Access SET IsActive = 'Y', Updated = Sysdate, UpdatedBy = ").append(Env.getAD_User_ID(m_ctx));
						else if (actionType.equals(actionType_deactivate) && (((Boolean) table.getValueAt(i, idxColumnActive)).booleanValue()))
							sql.append("UPDATE ").append(tableName).append("_Access SET IsActive = 'N', Updated = Sysdate, UpdatedBy = ").append(Env.getAD_User_ID(m_ctx));

						if (sql.length() > 0) { // peut être vide (Ex: désactivation sur une ligne où IsActive = N)
							sql.append(" WHERE ").append(tableName).append("_ID = ").append(objectID).append(" AND AD_Role_ID = ").append(roleID);
							DB.executeUpdateEx(sql.toString(), null);	
						}
					}
				}
			}
			updateTableContent();
		}
	}

	public void tableChanged(WTableModelEvent event) {

		if (event.getModel() == tableModel) {

			int row = event.getFirstRow();
			int col = event.getColumn();

			if (row < 0)
				return;

			if (checkAllSelected && col == idxColumnSelect) {
				ListModelTable model = table.getModel();
				boolean rowUnSelected = false;
				for (int i = 0; i < model.getRowCount(); i++) {
					if ( ! (Boolean) model.getValueAt(i, idxColumnSelect) ) {
						rowUnSelected = true;
						break;
					}
				}
				bSelect.setPressed(! rowUnSelected);
			}
		}
	}

	void onbSelect() {
		ListModelTable model = table.getModel();
		int rows = model.getSize();
		Boolean selectAll = bSelect.isPressed() ? Boolean.FALSE : Boolean.TRUE;
		bSelect.setPressed(! bSelect.isPressed());
		checkAllSelected = false;
		for (int i = 0; i < rows; i++)
			model.setValueAt(selectAll, i, idxColumnSelect);
		checkAllSelected = true;
		table.setModel(model);
	}
}
