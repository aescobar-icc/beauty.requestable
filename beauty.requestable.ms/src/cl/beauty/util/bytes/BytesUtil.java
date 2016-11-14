package cl.beauty.util.bytes;

public class BytesUtil {
	
	public static boolean contains(byte[] content,byte[] sequence){
		return indexOf(0,content.length,content, sequence) != -1;
	}
	public static int indexOf(byte[] content,byte[] sequence){
		return indexOf(0,content.length, content, sequence);
	}
	public static int indexOf(int start,byte[] content,byte[] sequence){
		return indexOf(start,content.length, content, sequence);
	}
	public static int indexOf(int start,int end,byte[] content,byte[] sequence){
		int i,j;
		for(i=start;i<end && i<content.length;i++){
			for(j=0;j<sequence.length;j++){
				if(i+j >= content.length)
					return -1;
				if(content[i+j] != sequence[j])
					break;
				if(j == sequence.length-1)
					return i;				
			}
		}
		return -1;
	}

	public static byte[] subArray(int start,int end,byte[] content){
	    // create the result array
	    byte[] result = new byte[end-start];

        System.arraycopy(content, start, result, 0, result.length);
	    return result;
	}
	public static byte[] concat(byte[]...arrays){
	    // Determine the length of the result array
	    int totalLength = 0;
	    for (int i = 0; i < arrays.length; i++)
	    {
	        totalLength += arrays[i].length;
	    }

	    // create the result array
	    byte[] result = new byte[totalLength];

	    // copy the source arrays into the result array
	    int currentIndex = 0;
	    for (int i = 0; i < arrays.length; i++)
	    {
	    	System.out.println(new String(arrays[i]));
	        System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
	        currentIndex += arrays[i].length;
	    }

	    return result;
	}
}
