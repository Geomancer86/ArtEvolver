package old;

import java.util.Arrays;

public class IntArray {
	public int[] array;

	public IntArray() {
		this.array = new int[0];
	}

	public IntArray(int[] paramArrayOfInt) {
		this.array = Arrays.copyOf(paramArrayOfInt, paramArrayOfInt.length);
	}

	public void add(int paramInt) {
		int[] arrayOfInt = Arrays.copyOf(this.array, this.array.length + 1);
		arrayOfInt[this.array.length] = paramInt;
		this.array = arrayOfInt;
	}

	public void add(int paramInt1, int paramInt2) {
		int[] arrayOfInt = new int[this.array.length + 1];
		for (int i = 0; i < paramInt2; i++) {
			arrayOfInt[i] = this.array[i];
		}
		arrayOfInt[paramInt2] = paramInt1;
		for (int i = paramInt2; i < this.array.length; i++) {
			arrayOfInt[(i + 1)] = this.array[i];
		}
		this.array = arrayOfInt;
	}

	public void remove(int paramInt) {
		int[] arrayOfInt = new int[this.array.length - 1];
		for (int i = 0; i < paramInt; i++) {
			arrayOfInt[i] = this.array[i];
		}
		for (int i = paramInt + 1; i < this.array.length; i++) {
			arrayOfInt[(i - 1)] = this.array[i];
		}
		this.array = arrayOfInt;
	}

	public int length() {
		return this.array.length;
	}

	public void setArray(int[] paramArrayOfInt) {
		this.array = paramArrayOfInt;
	}
}
