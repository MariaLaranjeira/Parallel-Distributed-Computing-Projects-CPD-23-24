import java.util.Scanner;
import java.io.FileWriter;   
import java.io.IOException;  

class MatrixProduct {

    static FileWriter valuesFile;

    private static double onMult(int m_ar, int m_br) {

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
        return duration/1000000000.0;

    }

    private static double onMultLine(int m_ar, int m_br) {

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
        return duration/1000000000.0;

    }


    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int op = 1;
        int lin, col, blockSize;
        int sizeDiv;

        try {
             valuesFile = new FileWriter("valuesFile.csv", true);
        } catch (IOException e){
            System.out.println("An error occurred opening the file.");
            e.printStackTrace();
        }

        int onMultValues[7] = {600, 1000, 1400, 1800, 2200, 2600, 3000};
        
        for (int i = 0; i < 7; i++) {
            try {
                valuesFile.write("Java," + "OnMult," + onMultValues[i] + ",N/A");
            } catch (IOException e){
                System.out.println("An error occurred writing to the file.");
                e.printStackTrace();
            }

            for (int j = 0; j < 5; j++) {
                try {
                    valuesFile.write("," + onMult(onMultValues[i], onMultValues[i])+",N/A"+ ",N/A");
                } catch (IOException e){
                    System.out.println("An error occurred writing to the file.");
                    e.printStackTrace();
                }
            }

            try {
                valuesFile.write("\n");
            } catch (IOException e){
                System.out.println("An error occurred writing to the file.");
                e.printStackTrace();
            }
        }


        for (int i = 0; i < 2; i++) {

            try {
                valuesFile.write("Java," + "OnMultLine," + onMultValues[i] + ",N/A");
            } catch (IOException e){
                System.out.println("An error occurred writing to the file.");
                e.printStackTrace();
            }  
            for (int j = 0; j < 5; j++) {
                try {
                    valuesFile.write("," + onMultLine(onMultValues[i], onMultValues[i]) + ",N/A"+ ",N/A");
                } catch (IOException e){
                    System.out.println("An error occurred writing to the file.");
                    e.printStackTrace();
                }
            }

            try {
                valuesFile.write("\n");
            } catch (IOException e){
                System.out.println("An error occurred writing to the file.");
                e.printStackTrace();
            }
    
        }

        try {
            valuesFile.close();
        } catch (IOException e){
            System.out.println("An error occurred closing the file.");
            e.printStackTrace();
        }    


        /* 
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

        */
    }


}

