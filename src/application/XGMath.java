package application;

public interface XGMath
{
	static int linearIO(int i, int in_min, int in_max, int out_min, int out_max)
	{	float
			in = i - in_min,
			in_range = in_max - in_min,
			out_range = out_max - out_min;
		return(int)(in / in_range * out_range + out_min);
//		return((int)(((float)(i - in_min) / (float)(in_max - in_min) * (out_max - out_min)) + out_min)); original
	}

	static float linearIO(int i, int in_min, int in_max, float out_min, float out_max)
	{	float
			in = i - in_min,
			in_range = in_max - in_min,
			out_range = out_max - out_min;
		return in / in_range * out_range + out_min;
	}
//Test
	static void createTable(String[] args)
	{	int min = 0, max = 139, i;
		float f = 0f;
		for(i = min; i <= max; i++)
		{	f = linearIO(i, min, max, 0.1f, 36.2f);
			System.out.println("<item value=\"" + i + "\" name=\"" + String.format("%1$4.2f", f) + "\"/>");
		}
	}

	public static void main(String[] args)
	{	int
			minMSB = 126 << 14,
			maxMSB = 127 << 14,
			minLSB = 0 << 7,
			maxLSB = 0 << 7,
			minPRG = 0,
			maxPRG = 127;
		System.out.println("kit: " + (minMSB |= minLSB |= minPRG) + "..." + (maxMSB |= maxLSB |= maxPRG));
		minMSB = 0 << 14;
		maxMSB = 64 << 14;
		minLSB = 0 << 14;
		maxLSB = 127 << 7;
		minPRG = 0 ;
		maxPRG = 127;
		System.out.println("voice: " + (minMSB |= minLSB |= minPRG) + "..." + (maxMSB |= maxLSB |= maxPRG));
	}
}