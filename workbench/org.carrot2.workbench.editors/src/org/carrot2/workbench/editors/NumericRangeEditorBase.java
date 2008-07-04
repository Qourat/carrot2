package org.carrot2.workbench.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;

/**
 * Common class for both floating point (with arbitrary precision of decimal digits) and
 * integer numeric ranges. The numeric widget contains a {@link Scale} and a
 * {@link Spinner}, both of which can be used to modify the current value of the editor.
 */
public abstract class NumericRangeEditorBase extends AttributeEditorAdapter implements
    IAttributeEditor
{
    /** */
    private Scale scale;

    /** */
    private Spinner spinner;

    /**
     * A temporary flag used to avoid event looping.
     */
    private boolean duringSelection;

    /*
     * Numeric ranges.
     */

    private int min;
    private int max;
    private int precisionDigits;

    private int increment;
    private int pageIncrement;

    private boolean minBounded;
    private boolean maxBounded;

    /**
     * Value multiplier needed to convert between fixed precision floating point
     * values and integers.
     */
    private double multiplier;

    /** Tooltip with allowed range. */
    private String tooltip;

    /**
     * Initialize numeric ranges, according to the descriptor's definition.
     */
    protected final void setRanges(int min, int max, int precisionDigits, int increment,
        int pageIncrement)
    {
        this.min = min;
        this.minBounded = (min != Integer.MIN_VALUE);

        this.max = max;
        this.maxBounded = (max != Integer.MAX_VALUE);

        this.increment = increment;
        this.pageIncrement = pageIncrement;

        this.precisionDigits = precisionDigits;

        this.multiplier = Math.pow(10, precisionDigits);

        if (!minBounded && !maxBounded)
        {
            this.tooltip = "Valid range: unbounded";
        }
        else
        {
            this.tooltip = "Valid range: ["
                + to_s(min) + "; " + to_s(max) + "]";
        }
    }

    /*
     * Return the current editor value.
     */
    @Override
    public Object getValue()
    {
        return spinner.getSelection();
    }

    /*
     * 
     */
    @Override
    public void createEditor(Composite parent, Object layoutData)
    {
        final int MIN_SPINNER_SIZE = 50;

        final Composite holder = new Composite(parent, SWT.NULL);
        holder.setLayoutData(layoutData);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;

        final GridData scaleLayoutData = new GridData();
        final GridData spinnerLayoutData = new GridData();

        if (minBounded && maxBounded)
        {
            layout.numColumns = 2;

            scaleLayoutData.horizontalAlignment = SWT.FILL;
            scaleLayoutData.grabExcessHorizontalSpace = true;

            createScale(holder);
            scale.setLayoutData(scaleLayoutData);
            
            spinnerLayoutData.grabExcessHorizontalSpace = false;            
        }
        else
        {
            layout.numColumns = 1;

            spinnerLayoutData.grabExcessHorizontalSpace = true;
        }

        createSpinner(holder);
        spinner.setSelection(spinner.getMaximum());
        spinnerLayoutData.minimumWidth = Math.max(MIN_SPINNER_SIZE, spinner.computeSize(
            SWT.DEFAULT, SWT.DEFAULT).x);
        spinnerLayoutData.horizontalAlignment = SWT.FILL;

        spinner.setLayoutData(spinnerLayoutData);

        holder.setLayout(layout);
    }

    /**
     * Create the scale control.
     */
    private void createScale(Composite holder)
    {
        scale = new Scale(holder, SWT.HORIZONTAL);

        scale.setMinimum(min);
        scale.setMaximum(max);
        scale.setIncrement(increment);
        scale.setPageIncrement(pageIncrement);
        scale.setToolTipText(tooltip);        

        /*
         * Hook event listener to the scale component.
         */
        scale.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                propagateNewValue(scale.getSelection());
            }
        });
    }

    /**
     * Create the spinner control.
     */
    private void createSpinner(Composite holder)
    {
        spinner = new Spinner(holder, SWT.BORDER);

        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setDigits(precisionDigits);
        spinner.setToolTipText(tooltip);

        if (minBounded && maxBounded)
        {
            spinner.setIncrement(increment);
            spinner.setPageIncrement(pageIncrement);
        }

        spinner.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                propagateNewValue(spinner.getSelection());
            }
        });
    }

    /**
     * Propagates value change event to all listeners.
     */
    protected final void propagateNewValue(int value)
    {
        if (!this.duringSelection)
        {
            this.duringSelection = true;

            if (spinner != null && spinner.getSelection() != value)
            {
                spinner.setSelection(value);
            }

            if (scale != null && scale.getSelection() != value)
            {
                scale.setSelection(value);
            }

            this.duringSelection = false;

            fireAttributeChange(new AttributeChangedEvent(this));
        }
    }
    
    /**
     * Convert between double values and integer values (taking into
     * account precision shift).
     */
    protected final int to_i(double v)
    {
        if (v == Double.MIN_VALUE)
        {
            return Integer.MIN_VALUE;
        }

        if (v == Double.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }

        if (Double.isNaN(v))
        {
            return 0;
        }

        return (int) Math.round(v * multiplier);
    }

    /**
     * Convert between double values and integer values (taking into
     * account precision shift).
     */
    protected final double to_d(int i)
    {
        return i / multiplier;
    }
    
    /*
     * Converts the given argument to a human-readable string.
     */
    private String to_s(int v)
    {
        if (v == Integer.MIN_VALUE)
        {
            return "-\u221E";
        }
        else if (v == Integer.MAX_VALUE)
        {
            return "\u221E";
        }
        else
        {
            return String.format("%." + precisionDigits + "f", to_d(v));
        }
    }
}
