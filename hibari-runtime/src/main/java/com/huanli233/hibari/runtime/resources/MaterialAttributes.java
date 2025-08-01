package com.huanli233.hibari.runtime.resources;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

public class MaterialAttributes {

  /**
   * Returns the {@link TypedValue} for the provided {@code attributeResId} or null if the attribute
   * is not present in the current theme.
   */
  @Nullable
  public static TypedValue resolve(@NonNull Context context, @AttrRes int attributeResId) {
    TypedValue typedValue = new TypedValue();
    if (context.getTheme().resolveAttribute(attributeResId, typedValue, true)) {
      return typedValue;
    }
    return null;
  }

  @NonNull
  public static TypedValue resolveTypedValueOrThrow(
          @NonNull Context context, @AttrRes int attributeResId) {
    return resolveTypedValueOrThrow(context, attributeResId, "");
  }

  @NonNull
  public static TypedValue resolveTypedValueOrThrow(
          @NonNull View componentView, @AttrRes int attributeResId) {
    return resolveTypedValueOrThrow(
        componentView.getContext(), attributeResId, componentView.getClass().getCanonicalName());
  }

  @NonNull
  public static TypedValue resolveTypedValueOrThrow(
      @NonNull Context context,
      @AttrRes int attributeResId,
      @NonNull String errorMessageComponent) {
    TypedValue typedValue = resolve(context, attributeResId);
    if (typedValue == null) {
      String errorMessage =
          "%1$s requires a value for the %2$s attribute to be set in your app theme. "
              + "You can either set the attribute in your theme or "
              + "update your theme to inherit from Theme.MaterialComponents (or a descendant).";
      throw new IllegalArgumentException(
          String.format(
              errorMessage,
              errorMessageComponent,
              context.getResources().getResourceName(attributeResId)));
    }
    return typedValue;
  }

  /**
   * Returns the {@link TypedValue} for the provided {@code attributeResId}.
   *
   * @throws IllegalArgumentException if the attribute is not present in the current theme.
   */
  public static int resolveOrThrow(
      @NonNull Context context,
      @AttrRes int attributeResId,
      @NonNull String errorMessageComponent) {
    return resolveTypedValueOrThrow(context, attributeResId, errorMessageComponent).data;
  }

  /**
   * Returns the {@link TypedValue} for the provided {@code attributeResId}, using the context of
   * the provided {@code componentView}.
   *
   * @throws IllegalArgumentException if the attribute is not present in the current theme.
   */
  public static int resolveOrThrow(
      @NonNull View componentView, @AttrRes int attributeResId) {
    return resolveTypedValueOrThrow(componentView, attributeResId).data;
  }

  /**
   * Returns the boolean value for the provided {@code attributeResId}.
   *
   * @throws IllegalArgumentException if the attribute is not present in the current theme.
   */
  public static boolean resolveBooleanOrThrow(
      @NonNull Context context,
      @AttrRes int attributeResId,
      @NonNull String errorMessageComponent) {
    return resolveOrThrow(context, attributeResId, errorMessageComponent) != 0;
  }

  /**
   * Returns the boolean value for the provided {@code attributeResId} or {@code defaultValue} if
   * the attribute is not a boolean or not present in the current theme.
   */
  public static boolean resolveBoolean(
      @NonNull Context context, @AttrRes int attributeResId, boolean defaultValue) {
    TypedValue typedValue = resolve(context, attributeResId);
    return (typedValue != null && typedValue.type == TypedValue.TYPE_INT_BOOLEAN)
        ? typedValue.data != 0
        : defaultValue;
  }

  /**
   * Returns the integer value for the provided {@code attributeResId} or {@code defaultValue} if
   * the attribute is not a integer or not present in the current theme.
   */
  public static int resolveInteger(
      @NonNull Context context, @AttrRes int attributeResId, int defaultValue) {
    TypedValue typedValue = resolve(context, attributeResId);
    return (typedValue != null && typedValue.type == TypedValue.TYPE_INT_DEC)
        ? typedValue.data
        : defaultValue;
  }

  /**
   * Returns the pixel value of the dimension specified by {@code attributeResId}. Defaults to
   * {@code defaultDimenResId} if {@code attributeResId} cannot be found or is not a dimension
   * within the given {@code context}.
   */
  @Px
  public static int resolveDimension(
      @NonNull Context context, @AttrRes int attributeResId, @DimenRes int defaultDimenResId) {
    TypedValue dimensionValue = resolve(context, attributeResId);
    if (dimensionValue == null || dimensionValue.type != TypedValue.TYPE_DIMENSION) {
      return (int) context.getResources().getDimension(defaultDimenResId);
    } else {
      return (int) dimensionValue.getDimension(context.getResources().getDisplayMetrics());
    }
  }
}