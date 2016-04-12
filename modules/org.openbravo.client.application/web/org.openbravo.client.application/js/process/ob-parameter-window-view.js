/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distribfuted  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.defineClass('OBParameterWindowView', isc.VLayout);

// == OBParameterWindowView ==
//   OBParameterWindowView is the view that represents parameter windows, this
//   is, Process Definition with Standard UIPattern. It contains a series of 
//   parameters (fields) and, optionally, a grid.
isc.OBParameterWindowView.addProperties({
  // Set default properties for the OBPopup container
  showMinimizeButton: true,
  showMaximizeButton: true,
  popupWidth: '90%',
  popupHeight: '90%',
  // Set later inside initWidget
  firstFocusedItem: null,

  // Set now pure P&E layout properties
  width: '100%',
  height: '100%',
  overflow: 'auto',
  autoSize: false,

  dataSource: null,

  viewGrid: null,

  addNewButton: null,

  isReport: false,
  reportId: null,
  pdfExport: false,
  xlsExport: false,

  gridFields: [],
  members: [],

  // allows to calculate extra context info (ie. when invoking from menu)
  additionalContextInfo: {},

  initWidget: function () {
    var i, field, items = [],
        buttonLayout = [],
        newButton, cancelButton, view = this,
        newShowIf, context, updatedExpandSection;

    // this flag can be used by Selenium to determine when defaults are set
    this.defaultsAreSet = false;

    // Buttons

    function actionClick() {
      view.setAllButtonEnabled(false);
      view.messageBar.hide();
      if (view.theForm) {
        view.theForm.errorMessage = '';
      }
      if (view.validate()) {
        view.doProcess(this._buttonValue);
      } else {
        // If the messageBar is visible, it means that it has been set due to a custom validation inside view.validate()
        // so we don't want to overwrite it with the generic OBUIAPP_ErrorInFields message
        if (!view.messageBar.isVisible()) {
          if (view.theForm.errorMessage) {
            view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_FillMandatoryFields') + ' ' + view.theForm.errorMessage);
          } else {
            view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_ErrorInFields'));
          }
        }
        view.setAllButtonEnabled(view.allRequiredParametersSet());
      }
    }

    if (this.popup) {
      buttonLayout.push(isc.LayoutSpacer.create({}));
    }

    if (this.buttons && !isc.isA.emptyObject(this.buttons)) {
      for (i in this.buttons) {
        if (this.buttons.hasOwnProperty(i)) {

          newButton = isc.OBFormButton.create({
            title: this.buttons[i],
            realTitle: '',
            _buttonValue: i,
            click: actionClick
          });
          buttonLayout.push(newButton);
          OB.TestRegistry.register('org.openbravo.client.application.process.pickandexecute.button.' + i, newButton);

          // pushing a spacer
          if (this.popup) {
            buttonLayout.push(isc.LayoutSpacer.create({
              width: 32
            }));
          }
        }
      }
    } else {
      if (this.isReport) {
        if (this.pdfExport) {
          this.pdfButton = isc.OBFormButton.create({
            title: OB.I18N.getLabel('OBUIAPP_PDFExport'),
            realTitle: '',
            _buttonValue: 'PDF',
            click: actionClick
          });
          buttonLayout.push(this.pdfButton);
          if (this.popup) {
            buttonLayout.push(isc.LayoutSpacer.create({
              width: 32
            }));
          }
        }
        if (this.xlsExport) {
          this.xlsButton = isc.OBFormButton.create({
            title: OB.I18N.getLabel('OBUIAPP_XLSExport'),
            realTitle: '',
            _buttonValue: 'XLS',
            click: actionClick
          });
          buttonLayout.push(this.xlsButton);
          if (this.popup) {
            buttonLayout.push(isc.LayoutSpacer.create({
              width: 32
            }));
          }
        }
      } else {
        this.okButton = isc.OBFormButton.create({
          title: OB.I18N.getLabel('OBUIAPP_Done'),
          realTitle: '',
          _buttonValue: 'DONE',
          click: actionClick
        });

        buttonLayout.push(this.okButton);
        // TODO: check if this is used, and remove as it is already registered
        OB.TestRegistry.register('org.openbravo.client.application.process.pickandexecute.button.ok', this.okButton);
        if (this.popup) {
          buttonLayout.push(isc.LayoutSpacer.create({
            width: 32
          }));
        }
      }
    }

    if (this.popup) {
      this.cancelButton = isc.OBFormButton.create({
        process: this,
        title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
        realTitle: '',
        click: function () {
          if (this.process.isExpandedRecord) {
            this.process.callerField.grid.collapseRecord(this.process.callerField.record);
          } else {
            view.closeClick();
          }
        }
      });
      buttonLayout.push(this.cancelButton);
      buttonLayout.push(isc.LayoutSpacer.create({}));
      OB.TestRegistry.register('org.openbravo.client.application.ParameterWindow_Cancel_Button_' + this.processId, this.cancelButton);
      // TODO: check if this is used, and remove as it is already registered
      OB.TestRegistry.register('org.openbravo.client.application.process.pickandexecute.button.cancel', this.cancelButton);
    }

    if (!this.popup) {
      this.toolBarLayout = isc.OBToolbar.create({
        view: this,
        leftMembers: [{}],
        rightMembers: buttonLayout
      });
      // this.toolBarLayout.addMems(buttonLayout);
      this.members.push(this.toolBarLayout);
    }

    // Message bar
    this.messageBar = isc.OBMessageBar.create({
      visibility: 'hidden',
      view: this,
      show: function () {
        var showMessageBar = true;
        this.Super('show', arguments);
        view.resized(showMessageBar);
      },
      hide: function () {
        var showMessageBar = false;
        this.Super('hide', arguments);
        view.resized(showMessageBar);
      }
    });
    this.members.push(this.messageBar);

    newShowIf = function (item, value, form, values) {
      var currentValues, originalShowIfValue = false,
          parentContext;

      currentValues = isc.shallowClone(values) || {};
      if (isc.isA.emptyObject(currentValues) && form && form.view) {
        currentValues = isc.shallowClone(form.view.getCurrentValues());
      } else if (isc.isA.emptyObject(currentValues) && form && form.getValues) {
        currentValues = isc.shallowClone(form.getValues());
      }
      OB.Utilities.fixNull250(currentValues);
      parentContext = this.view.getUnderLyingRecordContext(false, true, true, true);

      try {
        if (isc.isA.Function(this.originalShowIf)) {
          originalShowIfValue = this.originalShowIf(item, value, form, currentValues, parentContext);
        } else {
          originalShowIfValue = isc.JSON.decode(this.originalShowIf);
        }
      } catch (_exception) {
        isc.warn(_exception + ' ' + _exception.message + ' ' + _exception.stack);
      }
      if (originalShowIfValue && item.getType() === 'OBPickEditGridItem') {
        // load the grid if it is being shown for the first time
        if (item.canvas && item.canvas.viewGrid && !isc.isA.ResultSet(item.canvas.viewGrid.data)) {
          if (item.defaultFilter !== null && !isc.isA.emptyObject(item.defaultFilter)) {
            // if it has a default filter, apply it and use it when filtering
            item.canvas.viewGrid.setFilterEditorCriteria(item.defaultFilter);
            item.canvas.viewGrid.filterByEditor();
          } else {
            // if it does not have a default filter, just refresh the grid
            item.canvas.viewGrid.refreshGrid();
          }
        }
      }
      if (this.view && this.view.theForm) {
        this.view.theForm.markForRedraw();
      }
      return originalShowIfValue;
    };

    // this function is only used in OBSectionItems that are collapsed originally
    // this is done to force the data fetch of its stored OBPickEditGridItems
    updatedExpandSection = function () {
      var i, itemName, item;
      this.originalExpandSection();
      for (i = 0; i < this.itemIds.length; i++) {
        itemName = this.itemIds[i];
        item = this.form.getItem(itemName);
        if (item.type === 'OBPickEditGridItem' && !isc.isA.ResultSet(item.canvas.viewGrid.data)) {
          item.canvas.viewGrid.fetchData(item.canvas.viewGrid.getCriteria());
        }
      }
    };
    // Parameters
    if (this.viewProperties.fields) {
      for (i = 0; i < this.viewProperties.fields.length; i++) {
        field = this.viewProperties.fields[i];
        field = isc.addProperties({
          view: this
        }, field);

        if (field.showIf) {
          field.originalShowIf = field.showIf;
          field.showIf = newShowIf;
        }
        if (field.onChangeFunction) {
          // the default
          field.onChangeFunction.sort = 50;

          OB.OnChangeRegistry.register(this.viewId, field.name, field.onChangeFunction, 'default');
        }

        if (field.type === 'OBSectionItem' && !field.sectionExpanded) {
          // modifies the expandSection function of OBSectionItems collapsed originally to avoid having 
          // unloaded grids when a section is expanded for the first time
          field.originalExpandSection = isc.OBSectionItem.getPrototype().expandSection;
          field.expandSection = updatedExpandSection;
        }
        items.push(field);

      }

      if (items.length !== 0) {
        // create form if there items to include
        this.theForm = isc.OBParameterWindowForm.create({
          paramWindow: this
        });
        // If there is only one paremeter, it is a grid and the window is opened in a popup, then the window is a P&E window
        if (items && items.length === 1 && items[0].type === 'OBPickEditGridItem' && this.popup) {
          this.isPickAndExecuteWindow = true;
        }
        this.theForm.setItems(items);
        this.theForm.setFieldSections();
        this.formContainerLayout = isc.OBFormContainerLayout.create({});
        this.formContainerLayout.addMember(this.theForm);
        this.members.push(this.formContainerLayout);
      }
    }


    if (this.popup) {
      if (this.isReport) {
        if (this.pdfExport) {
          this.firstFocusedItem = this.pdfButton;
        } else if (this.xlsExport) {
          this.firstFocusedItem = this.xlsButton;
        }
      } else {
        this.firstFocusedItem = this.okButton;
      }
      this.popupButtons = isc.OBFormContainerLayout.create({
        defaultLayoutAlign: 'center',
        align: 'center',
        width: '100%',
        height: OB.Styles.Process.PickAndExecute.buttonLayoutHeight,
        members: [isc.HLayout.create({
          width: 1,
          overflow: 'visible',
          styleName: this.buttonBarStyleName,
          height: this.buttonBarHeight,
          defaultLayoutAlign: 'center',
          members: buttonLayout
        })]
      });
      this.members.push(this.popupButtons);
      this.closeClick = function () {
        this.closeClick = function () {
          return true;
        }; // To avoid loop when "Super call"
        if (this.isExpandedRecord) {
          this.callerField.grid.collapseRecord(this.callerField.record);
        } else {
          this.parentElement.parentElement.closeClick(); // Super call
        }
      };
    }
    this.loading = OB.Utilities.createLoadingLayout(OB.I18N.getLabel('OBUIAPP_PROCESSING'));
    this.loading.hide();
    this.members.push(this.loading);
    this.Super('initWidget', arguments);

    context = this.getUnderLyingRecordContext(false, true, true, true);

    // allow to add external parameters
    isc.addProperties(context, this.externalParams);

    if (this.callerField && this.callerField.view && this.callerField.view.getContextInfo) {
      isc.addProperties(context || {}, this.callerField.view.getContextInfo(true /*excludeGrids*/ ));
    }

    OB.RemoteCallManager.call('org.openbravo.client.application.process.DefaultsProcessActionHandler', context, {
      processId: this.processId,
      windowId: this.windowId
    }, function (rpcResponse, data, rpcRequest) {
      view.handleDefaults(data);
    });

    OB.TestRegistry.register('org.openbravo.client.application.ParameterWindow_' + this.processId, this);
    OB.TestRegistry.register('org.openbravo.client.application.ParameterWindow_MessageBar_' + this.processId, this.messageBar);
    OB.TestRegistry.register('org.openbravo.client.application.ParameterWindow_Form_' + this.processId, this.theForm);
    OB.TestRegistry.register('org.openbravo.client.application.ParameterWindow_FormContainerLayout_' + this.processId, this.formContainerLayout);
    if (this.isReport) {
      if (this.pdfExport) {
        OB.TestRegistry.register('org.openbravo.client.application.ParameterWindow_PDF_Export_' + this.processId, this.pdfExport);
      }
      if (this.xlsExport) {
        OB.TestRegistry.register('org.openbravo.client.application.ParameterWindow_XLS_Export_' + this.processId, this.xlsExport);
      }
    } else {
      OB.TestRegistry.register('org.openbravo.client.application.ParameterWindow_OK_Button_' + this.processId, this.okButton);
    }

  },

  handleResponse: function (refreshParent, message, responseActions, retryExecution, data) {
    var window = this.parentWindow,
        tab = OB.MainView.TabSet.getTab(this.viewTabId),
        i, afterRefreshCallback, me = this;

    // change title to done
    if (tab) {
      tab.setTitle(OB.I18N.getLabel('OBUIAPP_ProcessTitle_Done', [this.tabTitle]));
    }

    if (data.showResultsInProcessView) {
      if (!this.resultLayout) {
        this.resultLayout = isc.HLayout.create({
          width: '100%',
          height: '*'
        });
        this.addMember(this.resultLayout);
      } else {
        // clear the resultLayout
        this.resultLayout.setMembers([]);
      }
    }

    this.setAllButtonEnabled(this.allRequiredParametersSet());
    this.showProcessing(false);
    if (message) {
      if (this.popup) {
        if (!retryExecution) {
          if (message.title) {
            this.buttonOwnerView.messageBar.setMessage(message.severity, message.title, message.text);
          } else {
            this.buttonOwnerView.messageBar.setMessage(message.severity, message.text);
          }
        } else {
          // Popup has no message bar, showing the message in a warn popup
          isc.warn(message.text);
        }
      } else {
        if (message.title) {
          this.messageBar.setMessage(message.severity, message.title, message.text);
        } else {
          this.messageBar.setMessage(message.severity, message.text);
        }
      }
    }

    if (!retryExecution) {
      this.disableFormItems();
    } else {
      // Show again all toolbar buttons so the process
      // can be called again
      if (this.toolBarLayout) {
        for (i = 0; i < this.toolBarLayout.children.length; i++) {
          if (this.toolBarLayout.children[i].show) {
            this.toolBarLayout.children[i].show();
          }
        }
      }
      if (this.popupButtons) {
        this.popupButtons.show();
      }
    }

    if (responseActions) {
      responseActions._processView = this;
      OB.Utilities.Action.executeJSON(responseActions, null, null, this);
    }

    if (this.popup && !retryExecution) {
      this.buttonOwnerView.setAsActiveView();
      afterRefreshCallback = function () {
        if (me.buttonOwnerView && isc.isA.Function(me.buttonOwnerView.refreshParentRecord) && isc.isA.Function(me.buttonOwnerView.refreshChildViews)) {
          me.buttonOwnerView.refreshParentRecord();
          me.buttonOwnerView.refreshChildViews();
          me.buttonOwnerView.toolBar.updateButtonState();
        }
      };
      if (refreshParent) {
        if (this.callerField && this.callerField.view && typeof this.callerField.view.onRefreshFunction === 'function') {
          // In this case we are inside a process called from another process, so we want to refresh the caller process instead of the main window.
          this.callerField.view.onRefreshFunction(this.callerField.view);
        } else {
          this.buttonOwnerView.refreshCurrentRecord(afterRefreshCallback);
        }
      }

      this.closeClick = function () {
        return true;
      }; // To avoid loop when "Super call"
      if (this.isExpandedRecord) {
        this.callerField.grid.collapseRecord(this.callerField.record);
      } else {
        this.parentElement.parentElement.closeClick(); // Super call
      }
    }
  },

  disableFormItems: function () {
    var i, params;
    if (this.theForm && this.theForm.getItems) {
      params = this.theForm.getItems();
      for (i = 0; i < params.length; i++) {
        if (params[i].disable) {
          params[i].disable();
        }
      }
    }
  },

  // dummy required by OBStandardView.prepareGridFields
  setFieldFormProperties: function () {},

  validate: function () {
    var viewGrid, validForm;
    if (this.theForm) {
      validForm = this.theForm.validate();
      if (!validForm) {
        return validForm;
      }
    }
    return true;
  },

  showProcessing: function (processing) {
    var i;
    if (processing) {
      if (this.theForm) {
        this.theForm.hide();
      }
      if (this.popupButtons) {
        this.popupButtons.hide();
      }

      if (this.toolBarLayout) {
        for (i = 0; i < this.toolBarLayout.children.length; i++) {
          if (this.toolBarLayout.children[i].hide) {
            this.toolBarLayout.children[i].hide();
          }
        }
      }

      this.loading.show();
    } else {
      if (this.theForm) {
        this.theForm.show();
      }

      this.loading.hide();
    }
  },

  doProcess: function (btnValue) {
    var i, tmp, view = this,
        grid, allProperties = this.getUnderLyingRecordContext(false, true, false, true),
        selection, len, allRows, params, tab, actionHandlerCall, clientSideValidationFail;
    // activeView = view.parentWindow && view.parentWindow.activeView,  ???.
    if (this.resultLayout && this.resultLayout.destroy) {
      this.resultLayout.destroy();
      delete this.resultLayout;
    }
    // change tab title to show executing...
    tab = OB.MainView.TabSet.getTab(this.viewTabId);
    if (tab) {
      tab.setTitle(OB.I18N.getLabel('OBUIAPP_ProcessTitle_Executing', [this.tabTitle]));
    }

    allProperties._buttonValue = btnValue || 'DONE';

    allProperties._params = this.getContextInfo();

    // allow to add external parameters
    isc.addProperties(allProperties._params, this.externalParams);

    actionHandlerCall = function () {
      view.showProcessing(true);
      OB.RemoteCallManager.call(view.actionHandler, allProperties, {
        processId: view.processId,
        reportId: view.reportId,
        windowId: view.windowId
      }, function (rpcResponse, data, rpcRequest) {
        view.handleResponse(!(data && data.refreshParent === false), (data && data.message), (data && data.responseActions), (data && data.retryExecution), data);
      });
    };

    if (this.clientSideValidation) {
      clientSideValidationFail = function () {
        view.setAllButtonEnabled(view.allRequiredParametersSet());
      };
      this.clientSideValidation(this, actionHandlerCall, clientSideValidationFail);
    } else {
      actionHandlerCall();
    }
  },

  handleDefaults: function (result) {
    var i, field, def, defaults = result.defaults,
        filterExpressions = result.filterExpressions,
        defaultFilter = {},
        gridsToBeFiltered = [],
        allRequiredSet;
    if (!this.theForm) {
      if (this.onLoadFunction) {
        this.onLoadFunction(this);
      }
      return;
    }

    for (i in defaults) {
      if (defaults.hasOwnProperty(i)) {
        def = defaults[i];
        field = this.theForm.getItem(i);
        if (field) {
          if (isc.isA.Object(def)) {
            if (def.identifier && def.value) {
              field.valueMap = field.valueMap || {};
              field.valueMap[def.value] = def.identifier;
              field.setValue(def.value);
            }
          } else {
            field.setValue(this.getTypeSafeValue(field.typeInstance, def));
          }
        }
      }
    }
    for (i in filterExpressions) {
      if (filterExpressions.hasOwnProperty(i)) {
        field = this.theForm.getItem(i);
        defaultFilter = {};
        isc.addProperties(defaultFilter, filterExpressions[i]);
        field.setDefaultFilter(defaultFilter);
        if (field.isVisible() && !field.showIf) {
          field.canvas.viewGrid.setFilterEditorCriteria(defaultFilter);
          gridsToBeFiltered.push(field.canvas.viewGrid);
        }
      }
    }


    if (this.onLoadFunction) {
      this.onLoadFunction(this);
    }

    // filter after applying the onLoadFunction, just in case it has modified the filter editor criteria of a grid.
    // this way it a double requests for these grids is avoided
    for (i = 0; i < gridsToBeFiltered.length; i++) {
      gridsToBeFiltered[i].filterByEditor();
    }

    this.handleReadOnlyLogic();
    this.handleButtonsStatus();

    // redraw to execute display logic
    this.theForm.markForRedraw();
    this.handleDisplayLogicForGridColumns();

    // this flag can be used by Selenium to determine when defaults are set
    this.defaultsAreSet = true;
  },

  /**
   * Given a value, it returns the proper value according to the provided type
   */
  getTypeSafeValue: function (type, value) {
    var isNumber;
    if (!type) {
      return value;
    }
    isNumber = isc.SimpleType.inheritsFrom(type, 'integer') || isc.SimpleType.inheritsFrom(type, 'float');
    if (isNumber && isc.isA.Number(value)) {
      return value;
    } else if (isNumber && OB.Utilities.Number.IsValidValueString(type, value)) {
      return OB.Utilities.Number.OBMaskedToJS(value, type.decSeparator, type.groupSeparator);
    } else if (isNumber && isc.isA.Number(OB.Utilities.Number.OBMaskedToJS(value, '.', ','))) {
      // it might happen that default value uses the default '.' and ',' as decimal and group separator
      return OB.Utilities.Number.OBMaskedToJS(value, '.', ',');
    } else {
      return value;
    }
  },


  // Checks params with readonly logic enabling or disabling them based on it
  handleReadOnlyLogic: function () {
    var form, fields, i, field, parentContext;

    form = this.theForm;
    if (!form) {
      return;
    }
    parentContext = this.getUnderLyingRecordContext(false, true, true, true);

    fields = form.getFields();
    for (i = 0; i < fields.length; i++) {
      field = form.getField(i);
      if (field.readOnlyIf && field.setDisabled) {
        field.setDisabled(field.readOnlyIf(form.getValues(), parentContext));
      }
    }
  },

  handleDisplayLogicForGridColumns: function () {
    var form, fields, i, field;

    form = this.theForm;
    if (!form) {
      return;
    }

    fields = form.getFields();
    for (i = 0; i < fields.length; i++) {
      field = form.getField(i);
      if (field.canvas) {
        if (field.canvas.viewGrid) {
          field.canvas.viewGrid.evaluateDisplayLogicForGridColumns();
        }
      }
    }
  },

  getContextInfo: function (excludeGrids) {
    var result = {},
        params, i;
    if (!this.theForm) {
      return result;
    }

    if (this.theForm && this.theForm.getItems) {
      params = this.theForm.getItems();
      for (i = 0; i < params.length; i++) {
        if (excludeGrids && params[i].type === 'OBPickEditGridItem') {
          continue;
        }
        result[params[i].name] = params[i].getValue();
      }
    }

    return result;
  },

  getUnderLyingRecordContext: function (onlySessionProperties, classicMode, forceSettingContextVars, convertToClassicFormat) {
    var ctxInfo = (this.buttonOwnerView && this.buttonOwnerView.getContextInfo(onlySessionProperties, classicMode, forceSettingContextVars, convertToClassicFormat)) || {};
    return isc.addProperties(ctxInfo, this.additionalContextInfo);
  },


  setAllButtonEnabled: function (enabled) {
    if (this.isReport) {
      if (this.pdfExport) {
        this.pdfButton.setEnabled(enabled);
      }
      if (this.xlsExport) {
        this.xlsButton.setEnabled(enabled);
      }
    } else {
      if (this.okButton) {
        this.okButton.setEnabled(enabled);
      }
    }
  },

  handleButtonsStatus: function () {
    var allRequiredSet = this.allRequiredParametersSet();
    this.setAllButtonEnabled(allRequiredSet);
  },

  // returns true if any non-grid required parameter does not have a value
  allRequiredParametersSet: function () {
    var i, item, length = this.theForm && this.theForm.getItems().length,
        value, undef, nullValue = null;
    for (i = 0; i < length; i++) {
      item = this.theForm.getItems()[i];
      value = item.getValue();
      // Multiple selectors value is an array, check that it is not empty
      if (item.editorType === 'OBMultiSelectorItem' && value.length === 0) {
        value = null;
      }
      // do not take into account the grid parameters when looking for required parameters without value
      if (item.type !== 'OBPickEditGridItem' && item.required && item.isVisible() && value !== false && value !== 0 && !value) {
        return false;
      }
    }
    return true;
  }
});