import java.util.Scanner;

class MatrixProduct {

    private static void onMult(int m_ar, int m_br) {

        double[] pha = new double[m_ar*m_ar];
        double[] phb = new double[m_br*m_br];
        double[] phc = new double[m_ar*m_ar];
        int temp;
        int i;
        int j;
        int k;

        for(i = 0; i < m_ar; i++) {
            for(j  =0; j < m_ar; j++) {
                pha[i*m_ar + j] = 1.0;
            }    
        }
        
        for(i = 0; i < m_br; i++) {
            for(j  =0; j < m_br; j++) {
                phb[i*m_br + j] = i+1;
            }    
        }

        long startTime = System.nanoTime();

        for(i=0; i<m_ar; i++)
        {	for(j=0; j<m_br; j++)
            {	temp = 0;
                for(k=0; k<m_ar; k++)
                {	
                    temp += pha[i*m_ar + k] * phb[k*m_br + j];
                }
                phc[i*m_ar+j]=temp;
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println("Time: " + duration/1000000000.0);

    }

    private static void onMultLine(int m_ar, int m_br) {

        double[] pha = new double[m_ar*m_ar];
        double[] phb = new double[m_br*m_br];
        double[] phc = new double[m_ar*m_ar];
        int i;
        int j;
        int k;

        for(i = 0; i < m_ar; i++) {
            for(j  =0; j < m_ar; j++) {
                pha[i*m_ar + j] = 1.0;
            }    
        }
        
        for(i = 0; i < m_br; i++) {
            for(j  =0; j < m_br; j++) {
                phb[i*m_br + j] = i+1;
            }    
        }

        long startTime = System.nanoTime();

        for(i=0; i<m_ar; i++) {
            for(j=0; j<m_ar; j++ ) {
        	    for(k=0; k<m_br; k++) {
                    phc[i*m_ar + k] += pha[i*m_ar + j] * phb[j*m_br + k];
                }    
            }        
        }   
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println("Time: " + duration/1000000000.0);

    }

    private static void onMultBlock(int m_ar, int m_br, int bkSize) {

        double[] pha = new double[m_ar*m_ar];
        double[] phb = new double[m_br*m_br];
        double[] phc = new double[m_ar*m_ar];

        for(int i = 0; i < m_ar; i++) {
            for(int j  =0; j < m_ar; j++) {
                pha[i*m_ar + j] = 1.0;
            }    
        }
        
        for(int i = 0; i < m_br; i++) {
            for(int j  =0; j < m_br; j++) {
                phb[i*m_br + j] = i+1;
            }    
        }

        long startTime = System.nanoTime();

        for (int i0 = 0; i0 < m_ar; i0 += bkSize) {
		    for (int j0 = 0; j0 < m_ar; j0 += bkSize) {
			    for (int x = 0; x < m_ar; x++) {
				    for (int j = j0; j < Math.min(j0 + bkSize, m_ar); j++) {
					    for (int i = i0; i < Math.min(i0 + bkSize, m_ar); i++) {
						    phc[x*m_ar + i] += pha[x*m_ar + j] * phb[j*m_br + i];
                        }        
                    }        
                }            
            }                
        }
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println("Time: " + duration/1000000000.0);

    }


    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int op = 1;
        int lin, col, blockSize;

        do {
            System.out.println();
            System.out.println("=========- Java Program -=========");
            System.out.println("1. Multiplication");
            System.out.println("2. Line Multiplication");
            System.out.println("3. Block Multiplication");
            System.out.println("0. Exit");
            System.out.print("Selection?: ");
            op = scanner.nextInt();

            if (op == 0)
                break;

            System.out.print("Dimensions: lins=cols ? ");
            lin = scanner.nextInt();
            col = lin;

            switch (op) {
                case 1:
                    onMult(lin, col);
                    break;
                case 2:
                    onMultLine(lin, col);
                    break;
                case 3:
                    System.out.print("Block Size? ");
                    blockSize = scanner.nextInt();
                    onMultBlock(lin, col, blockSize);
                    break;
            }
        } while (op != 0);
    }


}

