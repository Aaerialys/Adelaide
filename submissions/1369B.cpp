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


int t, n;
string s;

int main(){
	ios_base::sync_with_stdio(0);
	cin.tie(0);
	cin>>t;
	while(t--){
		cin>>n;
		cin>>s;
		int a = -1, b = -1;
		for(int i = 0; i < n; i++){
			if(s[i] == '1' and b == -1)
				b = i;
			else if(s[i] == '0')
				a = i;
		}
		if(a == -1 or b == -1){
			cout<<s<<'\n';
			continue;
		}
		for(int i = 0; i < n; i++){
			if(s[i] == '1'){
				if(i > a)
					cout<<s[i];
			}
			else{
				if(i < b or i == a)
					cout<<s[i];
			}
		}
		cout<<'\n';
	}

}