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

int t, n, k, a[MM], w[MM];

int main(){
	scan(t);
	while(t--){
		scan(n, k);
		for(int i = 0; i < n; i++)
			scan(a[i]);
		for(int i = 0; i < k; i++)
			scan(w[i]);
		sort(a, a+n, greater<int>());
		sort(w, w+k);
		long long ans = 0;
		int j = k-1;
		for(int i = 0; i < k; i++){
			if(w[i] == 1){
				ans += a[i]*2;
				continue;
			}
			ans += a[i]; //maximum
			j += w[i]-1;
			ans += a[j];
		}
		print(ans);
	}
}