#include <bits/stdc++.h>
#define all(x) (x).begin(), (x).end()
#define gc getchar()
#define pc(x) putchar(x)
template<typename T> void scan(T &x){x = 0;bool _=0;T c=gc;_=c==45;c=_?gc:c;while(c<48||c>57)c=gc;for(;c<48||c>57;c=gc);for(;c>47&&c<58;c=gc)x=(x<<3)+(x<<1)+(c&15);x=_?-x:x;}
template<typename T> void printn(T n){bool _=0;_=n<0;n=_?-n:n;char snum[65];int i=0;do{snum[i++]=n%10+48;n/= 10;}while(n);--i;if (_)pc(45);while(i>=0)pc(snum[i--]);}
template<typename First, typename ... Ints> void scan(First &arg, Ints&... rest){scan(arg);scan(rest...);}
template<typename T> void print(T n){printn(n);pc(10);}
template<typename First, typename ... Ints> void print(First arg, Ints... rest){printn(arg);pc(32);print(rest...);}

using namespace std;
const int MM = 2e5+5;

int n, k, a[MM];

bool go(int v){
	int cnta = 0, cntb = 0;
	bool f = (k%2); //if odd
	
	for(int i = 1; i <= n-!f; i++){
		if(a[i] <= v){
			cnta++;
			i++;
		}
	}
	for(int i = 2; i <= n-f; i++){
		if(a[i] <= v){
			cntb++;
			i++;
		}
	}
	
	return (cnta >= (k+1)/2 or cntb >= k/2);
}

int main(){
	scan(n, k);
	for(int i = 1; i <= n; i++)
		scan(a[i]);
	int l = 1, m, r = 1e9;
	while(l <= r){
		m = (l+r)/2;
		if(go(m))
			r = m-1;
		else
			l = m+1;
	}
	print(l);
}