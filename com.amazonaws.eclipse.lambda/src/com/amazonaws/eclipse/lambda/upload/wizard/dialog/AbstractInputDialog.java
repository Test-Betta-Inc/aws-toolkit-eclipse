package com.amazonaws.eclipse.lambda.upload.wizard.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

abstract class AbstractInputDialog extends TitleAreaDialog {

    private final String title;
    private final String message;
    private final String messageDuringAction;
    private final String inputFieldLabel;
    private final String defaultInputValue;

    private Text inputText;
    private ProgressBar progressBar;

    /**
     * We store it memory so that we still have access to the input if the text
     * field has been disposed.
     */
    protected String inputValue;

    /**
     * Invoked after the user clicks OK button. Note this method is not called
     * in the main thread. So any UI update in this method should be guarded by
     * Display.syncExec or Display.asyncExec.
     */
    protected abstract void performFinish(String input);

    protected AbstractInputDialog(Shell parentShell, String title,
            String message, String messageDuringAction, String inputFieldLabel,
            String defaultInputValue) {
        super(parentShell);

        this.title = title;
        this.message = message;
        this.messageDuringAction = messageDuringAction;
        this.inputFieldLabel = inputFieldLabel;
        this.defaultInputValue = defaultInputValue;

        inputValue = defaultInputValue;
    }

    @Override
    public void create() {
      super.create();
      setTitle(title);
      setMessage(message, IMessageProvider.NONE);
    }


    @Override
    protected Control createDialogArea(Composite parent) {
      Composite area = (Composite) super.createDialogArea(parent);

      Composite container = new Composite(area, SWT.NONE);
      container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      GridLayout layout = new GridLayout(2, false);
      layout.marginTop = 20;
      layout.verticalSpacing = 40;
      container.setLayout(layout);

      Label label = new Label(container, SWT.NONE);
      label.setText(inputFieldLabel);

      inputText = new Text(container, SWT.BORDER);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      inputText.setLayoutData(gridData);
      inputText.setText(defaultInputValue);

      inputText.addModifyListener(new ModifyListener() {

        public void modifyText(ModifyEvent arg0) {
            inputValue = inputText.getText();

            if (inputValue.isEmpty()) {
                getButton(IDialogConstants.OK_ID).setEnabled(false);
            } else {
                getButton(IDialogConstants.OK_ID).setEnabled(true);
            }
        }
      });

      progressBar = new ProgressBar(container, SWT.INDETERMINATE);
      progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      GridData gd = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
      gd.horizontalSpan = 2;
      progressBar.setLayoutData(gd);

      progressBar.setVisible(false);

      return container;
    }

    @Override
    protected void okPressed() {
        setErrorMessage(null);
        setMessage(messageDuringAction);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        getButton(IDialogConstants.CANCEL_ID).setEnabled(false);

        progressBar.setVisible(true);

        new Thread(new Runnable() {

            public void run() {
                try {
                    performFinish(AbstractInputDialog.this.inputValue);
                } catch (final Exception e) {
                    // hide progress bar
                    Display.getDefault().syncExec(new Runnable() {
                        public void run() {
                            setErrorMessage(e.getMessage());
                            progressBar.setVisible(false);
                            getButton(IDialogConstants.OK_ID).setEnabled(true);
                            getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
                        }
                    });
                    return;
                }

                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        AbstractInputDialog.super.okPressed();
                    }
                });
            }
        }).start();

    }
}
