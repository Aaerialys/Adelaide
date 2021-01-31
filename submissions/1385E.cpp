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
using pii = pair<int, int>;
const int MM = 2e5+5;

int t, n, m, vis[MM], used[MM], c[MM], a[MM], b[MM], in[MM];
vector<int> adj[MM], sec[MM];
int no;

void dfs(int cur){
	vis[cur] = 1;
	for(int u: adj[cur]){
		if(!vis[u]){
			dfs(u);
		}
		else if(vis[u] == 1){
			no = 1;
		}
	}
	vis[cur] = 2;
}

int main(){
	scan(t);
	while(t--){
		no = 0;
		scan(n, m);
		for(int i = 1; i <= n; i++){
			adj[i].clear();
			sec[i].clear();
			vis[i] = 0;
			in[i] = 0;
		}
		for(int i = 0; i < m; i++){
			scan(c[i], a[i], b[i]);
			used[i] = 0;
			if(c[i]){
				adj[a[i]].emplace_back(b[i]);
				in[b[i]]++;
			}
			else{
				sec[a[i]].emplace_back(i);
				sec[b[i]].emplace_back(i);
			}
		}
		for(int i = 1; i <= n; i++){
			if(!vis[i]){
				dfs(i);
			}
		}
		
		if(no){
			puts("NO");
			continue;
		}

		//no existing cycles

		//topo sort, set all direction to cur first
		queue<int> q;
		for(int i = 1; i <= n; i++){
			if(!in[i]){
				vis[i] = 1;
				q.emplace(i);
			}
		}
		while(q.size()){
			int cur = q.front(); q.pop();
			for(int u: adj[cur]){
				if(!--in[u]){
					q.emplace(u);
				}
			}
			for(int i: sec[cur]){
				if(!used[i] and b[i] == cur){
					swap(a[i], b[i]);
				}
				used[i] = 1;
			}
		}
		puts("YES");
		for(int i = 0; i < m; i++){
			print(a[i], b[i]);
		}
	}
}