using System;
using System.Collections.Generic;
using System.Web;
using System.Text;
using System.Globalization;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using System.IO;
using System.Net;
using System.Net.Http;




class ICFPC2015
{
    [DataContract]
    class Data
    {
        public Data()
        {
        }

        [DataMember]
        public int id;
        [DataMember]
        public Unit[] units;
        [DataMember]
        public int width;
        [DataMember]
        public int height;
        [DataMember]
        public Cell[] filled;
        [DataMember]
        public int sourceLength;
        [DataMember]
        public int[] sourceSeeds;
    }

    [DataContract]
    class Unit
    {
        [DataMember]
        public Cell[] members;
        [DataMember]
        public Cell pivot;

        public Cell[,] allmember;
        public int MM;

        public void moveFirstPlace(int H, int W){
            int minY = 999;
            int minX = 999;
            int maxX = 0;
            foreach (var item in members)
            {
                minY = Math.Min(minY, item.y);
                maxX = Math.Max(maxX, item.x);
                minX = Math.Min(minX, item.x);
            }
            foreach (var item in members)
            {
                item.y -= minY;
                if (minY % 2 == 1)
                {
                    if (item.y % 2 == 0) item.x--;
                    else item.x++;
                }
            }
            pivot.y -= minY;

            int movex = 0;
            while ((minX + movex) / 2 < ((W - 1) - (maxX + movex)) / 2) movex -= 2;
            while ((minX + (movex + 2)) / 2 >= (((W - 1) - (maxX + (movex + 2)))) / 2) movex += 2;
            
            foreach (var item in members)
            {
                item.x += movex;
            }
            pivot.x += movex;

            foreach (var item in members)
            {
                item.x -= pivot.x;
                item.y -= pivot.y;
            }
            allmember = new Cell[members.Length, 6];
            List<int>[] l = new List<int>[6];

            for (int i = 0; i < members.Length; i++)
            {
                allmember[i, 0] = members[i];
                for (int j = 1; j < 6; j++)
                {
                    int px = allmember[i, j-1].x;
                    int py = allmember[i, j-1].y;
                    allmember[i, j] = new Cell((px + 3 * py) / 2, (px + py) / 2);
                }
            }

            for (int j = 0; j < 6; j++)
            {
                l[j] = new List<int>();
                for (int i = 0; i < members.Length; i++)
                {
                    l[j].Add(allmember[i, j].x * 1123139871 ^ allmember[j, i].y * 189628736);
                }
                l[j].Sort();
            }

            MM = 6;
            for (int j = 1; j < 6; j++)
            {
                bool flag = true;
                for (int i = 0; i < members.Length; i++)
                {
                    if (l[0][i] != l[j][i])
                    {
                        flag = false;
                        break;
                    }
                }
                if (flag)
                {
                    MM = j;
                    break;
                }
            }
        }
    }

    [DataContract]
    class Cell
    {
        public Cell(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        [DataMember]
        public int x;
        [DataMember]
        public int y;

        public void AdjustCell(int H, int W){
            x = 2 * x + (y % 2);
        }
    }

    [DataContract]
    class Output
    {
        [DataMember]
        public int problemId;
        [DataMember]
        public int seed;
        [DataMember]
        public string tag;
        [DataMember]
        public string solution;
    }

    ulong nowrand = 0;
    uint rand()
    {
        nowrand = nowrand * 1103515245 + 12345;
        nowrand &= uint.MaxValue;
        return (uint)nowrand;
    }


    class State
    {
        public int point;
        public int score;
        public int[,] board;
        public string command;


    }

    Random r;

    public ICFPC2015() { }

    public static void Main()
    {
        new ICFPC2015().myon();
    }

    HttpClient hc;

    int W, H, WW;
    int N;
    int M;
    int[] source;
    Data d;

    List<State> now;
    List<State> next;

    void myon()
    {
        string json = Console.ReadLine();
        var serializer = new DataContractJsonSerializer(typeof(Data));
        var stream1 = new MemoryStream(Encoding.UTF8.GetBytes(json));
        d = (Data)serializer.ReadObject(stream1);
        r = new Random();
        W = d.width;
        H = d.height;
        WW = W + W;

        foreach (var item in d.units)
        {
            foreach (var i2 in item.members)
            {
                i2.AdjustCell(H, WW);
            }
            item.pivot.AdjustCell(H, WW);
            item.moveFirstPlace(H, WW);
        }

        foreach (var item in d.filled)
        {
            item.AdjustCell(H, WW);
        }

        foreach (var seed in d.sourceSeeds)
        {
            nowrand = (ulong)seed;
            N = d.sourceLength;
            M = d.units.Length;
            source[0] = seed;
            for (int i = 1; i < N; i++)
            {
                source[i] = (int)(rand() % M);
            }
                
            State firststate = new State();
            firststate.board = new int[H, WW];
            firststate.command = "";
            firststate.score = 0;
            foreach (var item in d.filled)
            {
                firststate.board[item.x, item.y] = 1;
            }
            now = new List<State>();
            now.Add(firststate);
        }
    }

    int[] vy = new int[] { 0, 1, 1, 0 };
    int[] vx = new int[] { -2, -1, 1, 2 };
    char[] movec = new char[] { 'p', 'a', 'l', 'b' };

    int[] vp = new int[] { 1, -1 };
    char[] rotatec = new char[] { 'd', 'k' };

    void beamSearch()
    {
        for (int t = 0; t < d.sourceLength; t++)
        {
            int nextunit = source[t];
            var U = d.units[nextunit];
            int firsty = U.pivot.y + U.members[0].y;
            int firstx = U.pivot.x + U.members[0].x;

            foreach (var nowstate in now)
            {

                int MM = U.MM;

                bool[, ,] find = new bool[H, WW, MM];
                bool[, ,] check = new bool[H, WW, MM];
                string[, ,] addstring = new string[H, WW, MM];


                check[firsty, firstx, 0] = true;
                addstring[firsty, firstx, 0] = "";
                Queue<int> q = new Queue<int>();
                q.Enqueue((firstx << 16) + (firsty << 4) + 0);

                while (q.Count != 0)
                {
                    int nowpos = q.Dequeue();
                    int x = nowpos >> 16;
                    int y = (nowpos & 0xFFFF) >> 4;
                    int p = nowpos & 0xF;

                    int cx = x - U.allmember[0, p].x;
                    int cy = y - U.allmember[0, p].y;

                    if (!isSettable(U, nowstate, cx, cy, p))
                    {
                        //　突然の死！！！！
                        continue;
                    }

                    int stopmove = 0;

                    for (int k = 0; k < 4; k++)
                    {
                        int nx = x + vx[k];
                        int ny = y + vy[k];
                        int np = p;


                        if (!ok(nx, ny)) continue;
                        if (find[nx, ny, np])
                        {
                            if (check[nx, ny, np])
                            {
                                stopmove |= (1 << k);
                            }
                            continue;
                        }
                        find[nx, ny, np] = true;
                        
                        int ncx = cx + vx[k];
                        int ncy = cy + vy[k];

                        if (!isSettable(U, nowstate, ncx, ncy, np)) continue;
                        check[nx, ny, np] = true;
                        

                        addstring[nx, ny, np] = addstring[x, y, p] + movec[k];
                        int next = (nx << 16) + (ny << 4) + np;
                        q.Enqueue(next);
                    }

                    for (int k = 0; k < 2 && MM > 1; k++)
                    {
                        int np = (p + vp[k] + MM) % MM;
                        int nx = cx + U.allmember[0, np].x;
                        int ny = cy + U.allmember[0, np].y;

                        if (!ok(nx, ny)) continue;
                        if (find[nx, ny, np]) continue;
                        find[nx, ny, np] = true;

                        int ncx = cx;
                        int ncy = cy;

                        if (!isSettable(U, nowstate, ncx, ncy, np)) continue;
                        check[nx, ny, np] = true;
                    
                        addstring[nx, ny, np] = addstring[x, y, p] + movec[k];
                        int next = (nx << 16) + (ny << 4) + np;
                        q.Enqueue(next);
                    }

                    if (stopmove != (1 << 4) - 1)
                    {
                        //終了処理
                        int nextmove = 0;
                        while ((stopmove >> nextmove) % 2 == 1) nextmove++;


                    }
                }

            }
        }
    }

    bool isSettable(Unit U, State nowstate, int ncx, int ncy, int np)
    {
        for (int i = 0; i < M; i++)
        {
            int mx = U.allmember[i, np].x;
            int my = U.allmember[i, np].y;
            mx += ncx;
            my += ncy;
            if (!ok(mx, my))
            {
                return false;
            }
            if (nowstate.board[mx, my] == 1)
            {
                return false;
            }
        }
        return true;
    }

    //まだ書いてないけど書かなくてもいいかも
    void submit()
    {
        hc = new HttpClient();
        var content = new FormUrlEncodedContent(new Dictionary<string, string>
        {
            { "foo", "111" },
            { "bar", "222" },
            { "baz", "333" },
        });

        var response = hc.PostAsync("http://localhost/", content);
    }

    bool ok(int y, int x)
    {
        return y >= 0 && y < H && x >= 0 && x < W;
    }


    class Heap<T> where T : IComparable
    {
        public HeapNode<T> top;

        public Heap() { }

        public void push(T val)
        {
            top = HeapNode<T>.meld(top, new HeapNode<T>(val));
        }

        public T pop()
        {
            T ret = top.val;
            top = HeapNode<T>.meld(top.r, top.l);
            return ret;
        }

        public void merge(Heap<T> h2)
        {
            top = HeapNode<T>.meld(top, h2.top);
        }

        public class HeapNode<NT> where NT : IComparable
        {
            public HeapNode<NT> l, r;
            public NT val;

            public HeapNode(NT _val)
            {
                val = _val;
            }

            public static HeapNode<NT> meld(HeapNode<NT> a, HeapNode<NT> b)
            {
                if (a == null) return b;
                if (b == null) return a;
                if (a.val.CompareTo(b.val) > 0)
                {
                    HeapNode<NT> temp = a;
                    a = b;
                    b = temp;
                }
                a.r = meld(a.r, b);
                HeapNode<NT> temph = a.l;
                a.l = a.r;
                a.r = temph;
                return a;
            }
        }
    }


}

