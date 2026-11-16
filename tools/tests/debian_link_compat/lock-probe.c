#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
int main(int argc,char **argv) {
 if(argc!=2)return 2;
 char tmp[4096],lock[4096],data[32]={0};
 snprintf(tmp,sizeof(tmp),"%s/.tX1-lock",argv[1]);snprintf(lock,sizeof(lock),"%s/.X1-lock",argv[1]);
 unlink(tmp);unlink(lock);
 int fd=open(tmp,O_CREAT|O_EXCL|O_WRONLY,0644);if(fd<0){perror("create");return 2;}
 const char *pid="     12345\n";
 if(write(fd,pid,strlen(pid))!=(ssize_t)strlen(pid))return 2;
 close(fd);
 if(link(tmp,lock)){perror("Linking lock file in place failed");unlink(tmp);return 1;}
 struct stat st;if(stat(lock,&st)||st.st_nlink!=2){fprintf(stderr,"hard-link count incorrect\n");return 2;}
 unlink(tmp);fd=open(lock,O_RDONLY);if(fd<0)return 2;
 if(read(fd,data,sizeof(data)-1)<0)return 2;
 close(fd);unlink(lock);
 if(strcmp(data,pid))return 2;
 puts("Lock creation, link count and contents survived unlink of temporary file");return 0;
}
