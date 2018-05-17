package cruft.wtf.gimlet.ui.controls;

/**
 * This interface provides methods which can be used by the {@link cruft.wtf.gimlet.ui.dialog.ParamInputDialog},
 * to get and set parameters based on specific JavaFX controls in this package (to prevent casting and all).
 *
 * @param <T>
 */
public interface ParamInput<T> {

    /**
     * Gets the parameter name for the input dialog. Usually this is just the {@link javafx.scene.Node}'s ID.
     *
     * @return The parameter name.
     */
    String getParameterName();

    /**
     * Gets the parameter value which has been set in the control.
     *
     * @return The parameter value.
     */
    T getParameterValue();

    /**
     * Sets the parameter value.
     *
     * @param o The value.
     */
    void setParameterValue(T o);
}
