#include <bits/stdc++.h>
#define gc getchar()
#define pc(x) putchar(x)
template<typename T> void scan(T &x){x = 0;register bool _=0;register T c=gc;_=c==45;c=_?gc:c;while(c<48||c>57)c=gc;for(;c<48||c>57;c=gc);for(;c>47&&c<58;c=gc)x=(x<<3)+(x<<1)+(c&15);x=_?-x:x;}
template<typename T> void printn(T n){register bool _=0;_=n<0;n=_?-n:n;char snum[65];int i=0;do{snum[i++]=n%10+48;n/= 10;}while(n);--i;if (_)pc(45);while(i>=0)pc(snum[i--]);}
template<typename First, typename ... Ints> void scan(First &arg, Ints&... rest){scan(arg);scan(rest...);}
template<typename T> void print(T n){printn(n);pc(10);}
template<typename First, typename ... Ints> void print(First arg, Ints... rest){printn(arg);pc(32);print(rest...);}

using namespace std;
typedef long long ll;
const int MM = 101;

int t, n, k, a[MM];
ll m;

void go(){
    scan(n, m, k);
    for(int i = 0; i < n; i++)
        scan(a[i]);
    
    for(int i = 0; i < n-1; i++){
        int low = max(0, a[i+1] - k);
        
        if(a[i] > low)
            m += a[i] - low;
        if(a[i] < low){
            m -= low - a[i];
            if(m < 0){
                puts("NO");
                return;
            }
        }
        a[i] = low;
    }
    puts("YES");
}

int main(){
    
    scan(t);
    while(t--){
        go();
    }
    
    return 0;
}