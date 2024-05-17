package com.pistacium.modcheck.util

import java.awt.Container
import javax.swing.JComponent

/**
 * A collection of utility methods for Swing.
 *
 * @author Darryl Burke
 */
object SwingUtils {
    fun <T : JComponent?> getDescendantsOfType(clazz: Class<T>, container: Container): List<T> {
        return getDescendantsOfType(clazz, container, true)
    }

    fun <T : JComponent?> getDescendantsOfType(clazz: Class<T>, container: Container, nested: Boolean): List<T> {
        val tList: MutableList<T> = ArrayList()
        for (component in container.components) {
            if (clazz.isAssignableFrom(component.javaClass)) {
                tList.add(clazz.cast(component))
            }
            if (nested || !clazz.isAssignableFrom(component.javaClass)) {
                tList.addAll(getDescendantsOfType(clazz, component as Container, nested))
            }
        }
        return tList
    }
}
