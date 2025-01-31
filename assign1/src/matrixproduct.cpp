#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <omp.h>
#include <fstream>

using namespace std;

#define SYSTEMTIME clock_t

// Naive Matrix Multiplication Algorithm 
double OnMult(int m_ar, int m_br) 
{
	
	SYSTEMTIME Time1, Time2;
	
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	
		
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);


    Time1 = clock();

	for(i=0; i<m_ar; i++)
	{	for( j=0; j<m_br; j++)
		{	temp = 0;
			for( k=0; k<m_ar; k++)
			{	
				temp += pha[i*m_ar + k] * phb[k*m_br + j];
			}
			phc[i*m_ar+j]=temp;
		}
	}


    Time2 = clock();
	double res = (double)(Time2 - Time1) / CLOCKS_PER_SEC;

    free(pha);
    free(phb);
    free(phc);

	return res;
	
}

// Line Matrix Multiplication Algorithm
double OnMultLine(int m_ar, int m_br)
{
    SYSTEMTIME Time1, Time2;
    
	double temp;
    int i, j, k;

    double *pha, *phb, *phc;
    
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for(i=0; i<m_ar; i++)
        for(j=0; j<m_ar; j++)
            pha[i*m_ar + j] = (double)1.0;

    for(i=0; i<m_br; i++)
        for(j=0; j<m_br; j++)
            phb[i*m_br + j] = (double)(i+1);


    Time1 = clock();

    for(i=0; i<m_ar; i++)
        for( j=0; j<m_ar; j++ )
        	for( k=0; k<m_br; k++)
                phc[i*m_ar + k] += pha[i*m_ar + j] * phb[j*m_br + k];
            

    Time2 = clock();
	double res = (double)(Time2 - Time1) / CLOCKS_PER_SEC;

    free(pha);
    free(phb);
	free(phc);

	return res;

}

// Outer Parallelization of Line Matrix Multiplication Algorithm
double OnMultLineCores(int m_ar, int m_br)
{
    double Time1, Time2;
    
	double temp;
    int i, j, k;

    double *pha, *phb, *phc;
    
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for(i=0; i<m_ar; i++)
        for(j=0; j<m_ar; j++)
            pha[i*m_ar + j] = (double)1.0;

    for(i=0; i<m_br; i++)
        for(j=0; j<m_br; j++)
            phb[i*m_br + j] = (double)(i+1);


    Time1 = omp_get_wtime();

	#pragma omp parallel for private(i, j, k) shared(pha, phb, phc)
    for(i=0; i<m_ar; i++)
        for( j=0; j<m_ar; j++ )
        	for( k=0; k<m_br; k++)
                phc[i*m_ar + k] += pha[i*m_ar + j] * phb[j*m_br + k];
            

    Time2 =  omp_get_wtime();
	double res = (double)(Time2 - Time1);

    free(pha);
    free(phb);
	free(phc);

	return res;
}

// Outer and Inner Parallelization of Line Matrix Multiplication Algorithm
double OnMultLineCores2(int m_ar, int m_br)
{
    double Time1, Time2;
    
	double temp;
    int i, j, k;

    double *pha, *phb, *phc;
    
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for(i=0; i<m_ar; i++)
        for(j=0; j<m_ar; j++)
            pha[i*m_ar + j] = (double)1.0;

    for(i=0; i<m_br; i++)
        for(j=0; j<m_br; j++)
            phb[i*m_br + j] = (double)(i+1);


    Time1 = omp_get_wtime();

	#pragma omp parallel for private(i, j, k) shared(pha, phb, phc)
    for(i=0; i<m_ar; i++)
        for( j=0; j<m_ar; j++ )
			#pragma omp parallel for
        	for( k=0; k<m_br; k++)
                phc[i*m_ar + k] += pha[i*m_ar + j] * phb[j*m_br + k];
            

    Time2 =  omp_get_wtime();
	double res = (double)(Time2 - Time1);

    free(pha);
    free(phb);
	free(phc);

	return res;
}

// Block Multiplication Algorithm
double OnMultBlock(int m_ar, int m_br, int bkSize)
{
    
    SYSTEMTIME Time1, Time2;
	
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
		
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;


	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);	

	Time1 = clock();

	for (int i0 = 0; i0 < m_ar; i0 += bkSize)
		for (int j0 = 0; j0 < m_ar; j0 += bkSize)
			for (int x = 0; x < m_ar; x++)
				for (int j = j0; j < min(j0 + bkSize, m_ar); j++)
					for (int i = i0; i < min(i0 + bkSize, m_ar); i++)
						phc[x*m_ar + i] += pha[x*m_ar + j] * phb[j*m_br + i];
					
	Time2 = clock();
	double res = (double)(Time2 - Time1) / CLOCKS_PER_SEC;

    free(pha);
    free(phb);
    free(phc);

	return res;
    
}

void handle_error (int retval)
{
  printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
  exit(1);
}

void init_papi() {
  int retval = PAPI_library_init(PAPI_VER_CURRENT);
  if (retval != PAPI_VER_CURRENT && retval < 0) {
    printf("PAPI library version mismatch!\n");
    exit(1);
  }
  if (retval < 0) handle_error(retval);

  std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
            << " MINOR: " << PAPI_VERSION_MINOR(retval)
            << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}


int main (int argc, char *argv[])
{

	char c;
	int lin, col, blockSize;
	int op;
	
	int EventSet = PAPI_NULL;
  	long long values[2];
  	int ret;
	int sizeDiv;

	ofstream valuesFile;
	

	ret = PAPI_library_init( PAPI_VER_CURRENT );
	if ( ret != PAPI_VER_CURRENT )
		std::cout << "FAIL" << endl;


	ret = PAPI_create_eventset(&EventSet);
		if (ret != PAPI_OK) cout << "ERROR: create eventset" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L1_DCM );
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L1_DCM" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L2_DCM);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L2_DCM" << endl;

	int onMultValues[7] = {600, 1000, 1400, 1800, 2200, 2600, 3000};
	int onMultOptValues[11] = {600, 1000, 1400, 1800, 2200, 2600, 3000, 4096, 6144, 8192, 10240};

	valuesFile.open("valuesFile.csv");
	valuesFile << "Programming Language," << "Algorithm Name," << "Matrix Size," << "Block Size," << "Run 1," << "Run 1 L1," << "Run 1 L2," << "Run 2," << "Run 2 L1," << "Run 2 L2," << "Run 3," << "Run 3 L1," << "Run 3 L2," << "Run 4," << "Run 4 L1," << "Run 4 L2," << "Run 5," << "Run 5 L1," << "Run 5 L2" << endl;
	
	for (int i = 0; i < 7; i++) {
		valuesFile << "C++," << "OnMult," << onMultValues[i] << ",N/A";

		for (int j = 0; j < 5; j++) {
			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;
			valuesFile << "," << OnMult(onMultValues[i], onMultValues[i]);
			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
			valuesFile << "," << values[0] << "," << values[1];

		}
		valuesFile << endl;

	}
	

	for (int i = 0; i < 11; i++) {

		
		valuesFile << "C++," << "OnMultLine," << onMultOptValues[i] << ",N/A";
		for (int j = 0; j < 5; j++) {
			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;
			valuesFile << "," << OnMultLine(onMultOptValues[i], onMultOptValues[i]);
			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
			valuesFile << "," << values[0] << "," << values[1];
		}
		valuesFile << endl;

		valuesFile << "C++," << "OnMultLineCores," << onMultOptValues[i] << ",N/A";
		for (int j = 0; j < 5; j++) {
			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;
			valuesFile << "," << OnMultLineCores(onMultOptValues[i], onMultOptValues[i]);
			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
			valuesFile << "," << values[0] << "," << values[1];
		}
		valuesFile << endl;
		

		valuesFile << "C++," << "OnMultLineCores2," << onMultOptValues[i] << ",N/A";
		for (int j = 0; j < 5; j++) {
			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;
			valuesFile << "," << OnMultLineCores2(onMultOptValues[i], onMultOptValues[i]);
			ret = PAPI_stop(EventSet, values);
			if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
			valuesFile << "," << values[0] << "," << values[1];
		}
		valuesFile << endl;

		
		sizeDiv = 8;
		for (int k = 0; k < 3; k++) {
			valuesFile << "C++," << "OnMultBlock," << onMultOptValues[i];
			valuesFile << "," << onMultOptValues[i]/sizeDiv;

			for (int j = 0; j < 5; j++) {
				ret = PAPI_start(EventSet);
				if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;
				valuesFile << "," << OnMultBlock(onMultOptValues[i], onMultOptValues[i], onMultOptValues[i]/sizeDiv );
				ret = PAPI_stop(EventSet, values);
				if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
				valuesFile << "," << values[0] << "," << values[1];
			}
			sizeDiv /= 2;
			valuesFile << endl;
		}
	}
	
	valuesFile.close();

	ret = PAPI_reset( EventSet );
	if ( ret != PAPI_OK )
		std::cout << "FAIL reset" << endl; 


	ret = PAPI_remove_event( EventSet, PAPI_L1_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_remove_event( EventSet, PAPI_L2_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_destroy_eventset( &EventSet );
	if ( ret != PAPI_OK )
		std::cout << "FAIL destroy" << endl;

}